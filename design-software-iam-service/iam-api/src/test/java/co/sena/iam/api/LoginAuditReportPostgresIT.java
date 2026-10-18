package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HU-IAM-009 E1-E2 end to end against a real PostgreSQL carrying the iam-db schemas: the full
 * stack (auth -> authorization -> JPA read adapter -> REST envelope) for
 * {@code GET /reports/login-audit}.
 *
 * <p>Set {@code IAM_DB_DSN}, {@code IAM_DB_USERNAME} and {@code IAM_DB_PASSWORD} to run:
 *
 * <pre>
 * IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
 * IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... ./mvnw verify
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class LoginAuditReportPostgresIT {

    private static final String ADMIN_EMAIL = "hu-iam-009-admin@test.sena.edu.co";
    private static final String NO_GRANT_EMAIL = "hu-iam-009-noaccess@test.sena.edu.co";
    private static final String PASSWORD = "Integr4tion!Test";
    private static final String AUDIT_ROW_EMAIL = "hu-iam-009-audited@test.sena.edu.co";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private UUID adminId;
    private UUID noGrantId;

    @DynamicPropertySource
    static void jwtSigningKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", LoginAuditReportPostgresIT::ephemeralSigningKey);
        registry.add("iam.jwt.issuer", () -> "https://iam.it.sena.local");
    }

    @BeforeEach
    void setUp() {
        deleteTestData();
        adminId = createUser(ADMIN_EMAIL);
        noGrantId = createUser(NO_GRANT_EMAIL);
        assignRole(adminId, "SYSTEM_ADMIN", null);
    }

    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    // ---------------------------------------------------------------- E1

    @Test
    @DisplayName("E1 cursor pagination walks all rows, most recent first, without gaps or repeats")
    void paginatesByCursor() throws Exception {
        Instant base = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID oldest = insertAuditRow(base, "SUCCESS");
        UUID middle = insertAuditRow(base.plusSeconds(1), "INVALID_PASSWORD");
        UUID newest = insertAuditRow(base.plusSeconds(2), "USER_NOT_FOUND");

        String adminToken = login(ADMIN_EMAIL);

        JsonNode firstPage = json.readTree(mvc.perform(get("/api/v1/reports/login-audit")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("email", AUDIT_ROW_EMAIL)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.has_more").value(true))
                .andReturn().getResponse().getContentAsString());

        assertThat(firstPage.get("data").get(0).get("id").asText()).isEqualTo(newest.toString());
        assertThat(firstPage.get("data").get(1).get("id").asText()).isEqualTo(middle.toString());
        String cursor = firstPage.get("pagination").get("next_cursor").asText();

        mvc.perform(get("/api/v1/reports/login-audit")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("email", AUDIT_ROW_EMAIL)
                        .param("limit", "2")
                        .param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(oldest.toString()))
                .andExpect(jsonPath("$.pagination.has_more").value(false))
                .andExpect(jsonPath("$.pagination.next_cursor").doesNotExist());
    }

    @Test
    @DisplayName("E1 the outcome filter narrows the report to matching rows")
    void filtersByOutcome() throws Exception {
        Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
        insertAuditRow(now, "SUCCESS");
        insertAuditRow(now.plusSeconds(1), "ACCOUNT_LOCKED");

        String adminToken = login(ADMIN_EMAIL);

        mvc.perform(get("/api/v1/reports/login-audit")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("email", AUDIT_ROW_EMAIL)
                        .param("outcome", "ACCOUNT_LOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].outcome").value("ACCOUNT_LOCKED"));
    }

    // ---------------------------------------------------------------- E2

    @Test
    @DisplayName("E2 a caller without AUDIT_LOG_VIEW is refused with 403 FORBIDDEN")
    void refusesCallerWithoutGrant() throws Exception {
        String noGrantToken = login(NO_GRANT_EMAIL);

        mvc.perform(get("/api/v1/reports/login-audit")
                        .header("Authorization", "Bearer " + noGrantToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("E2 no Authorization header is refused with 401 UNAUTHENTICATED")
    void refusesMissingToken() throws Exception {
        mvc.perform(get("/api/v1/reports/login-audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    // ---------------------------------------------------------------- helpers

    private UUID createUser(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, ?, 'Audit', 'Probe', 'USER', TRUE)
                """, id, email, new BCryptPasswordEncoder().encode(PASSWORD));
        return id;
    }

    private void assignRole(UUID userId, String roleName, UUID trainingCenterId) {
        jdbc.update("""
                INSERT INTO rbac.user_role (user_id, role_id, training_center_id, assigned_by)
                SELECT ?, r.id, ?, ? FROM rbac.role r WHERE r.name = ?
                """, userId, trainingCenterId, userId, roleName);
    }

    private UUID insertAuditRow(Instant attemptedAt, String outcome) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity_audit.audit_login
                    (id, user_id, email_attempted, outcome, attempted_at)
                VALUES (?, NULL, ?, ?, ?)
                """, id, AUDIT_ROW_EMAIL, outcome, java.sql.Timestamp.from(attemptedAt));
        return id;
    }

    private String login(String email) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }

    private void deleteTestData() {
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", AUDIT_ROW_EMAIL);
        for (String email : new String[] {ADMIN_EMAIL, NO_GRANT_EMAIL}) {
            jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", email);
            for (UUID id : jdbc.queryForList(
                    "SELECT id FROM identity.\"user\" WHERE email = ?", UUID.class, email)) {
                jdbc.update("DELETE FROM rbac.user_role WHERE user_id = ?", id);
                jdbc.update("DELETE FROM session.refresh_token WHERE user_id = ?", id);
                jdbc.update("DELETE FROM identity.\"user\" WHERE id = ?", id);
            }
        }
    }

    private static String ephemeralSigningKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return Base64.getEncoder().encodeToString(gen.generateKeyPair().getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA required by JRE spec", e);
        }
    }
}
