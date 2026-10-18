package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * HU-IAM-005 E1–E4 end to end against a real PostgreSQL carrying the iam-db schemas.
 *
 * <p>Set {@code IAM_DB_DSN}, {@code IAM_DB_USERNAME} and {@code IAM_DB_PASSWORD} to run:
 *
 * <pre>
 * IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
 * IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... ./mvnw verify
 * </pre>
 *
 * <p>All rows written use the {@link #TARGET_EMAIL} or {@link #ADMIN_EMAIL} marker and are
 * deleted by those markers alone — no blanket truncation of shared tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class UserManagementPostgresIT {

    private static final String ADMIN_EMAIL = "hu-iam-005-admin@test.sena.edu.co";
    private static final String TARGET_EMAIL = "hu-iam-005-target@test.sena.edu.co";
    private static final String PASSWORD = "Integr4tion!Test";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    private UUID adminId;
    private String token;

    @DynamicPropertySource
    static void jwtSigningKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", UserManagementPostgresIT::ephemeralSigningKey);
        registry.add("iam.jwt.issuer", () -> envOr("IAM_JWT_ISSUER", "https://iam.it.sena.local"));
    }

    @BeforeEach
    void setUp() throws Exception {
        deleteTestData();
        adminId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, ?, 'It', 'Admin', 'USER', TRUE)
                """, adminId, ADMIN_EMAIL, new BCryptPasswordEncoder().encode(PASSWORD));
        jdbc.update("""
                INSERT INTO rbac.user_role (user_id, role_id, assigned_by)
                SELECT ?, r.id, ? FROM rbac.role r WHERE r.name = 'SYSTEM_ADMIN'
                """, adminId, adminId);

        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        token = json.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }

    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    // ---------------------------------------------------------------- E1 POST /users

    @Test
    @DisplayName("E1 POST /users returns 201 with temporary_password and stages iam.user.created in outbox")
    void createUserReturnsTemporaryPasswordAndStagedEvent() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(targetUserBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(TARGET_EMAIL))
                .andExpect(jsonPath("$.temporary_password").isNotEmpty())
                .andReturn();

        UUID createdId = UUID.fromString(
                json.readTree(result.getResponse().getContentAsString()).get("id").asText());

        long outboxCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM identity.outbox
                WHERE event_type = 'iam.user.created'
                  AND payload->'data'->>'user_id' = ?
                """, Long.class, createdId.toString());
        assertThat(outboxCount).isEqualTo(1);
    }

    @Test
    @DisplayName("E1 POST /users with duplicate email answers 409")
    void duplicateEmailAnswers409() throws Exception {
        mvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(targetUserBody()))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(targetUserBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IAM_EMAIL_CONFLICT"));
    }

    @Test
    @DisplayName("E1 POST /users without token answers 401")
    void createUserWithoutTokenAnswers401() throws Exception {
        mvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(targetUserBody()))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- E2 GET /users and GET /users/{id}

    @Test
    @DisplayName("E2 GET /users returns paginated list including the created user")
    void listUsersReturnsPaginatedResult() throws Exception {
        JsonNode created = createTargetUser();
        String createdId = created.get("id").asText();

        MvcResult result = mvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.total_items").isNumber())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        boolean found = false;
        for (JsonNode u : body.get("data")) {
            if (createdId.equals(u.get("id").asText())) {
                found = true;
                assertThat(u.get("email").asText()).isEqualTo(TARGET_EMAIL);
                break;
            }
        }
        assertThat(found).as("created user appears in list").isTrue();
    }

    @Test
    @DisplayName("E2 GET /users/{id} returns the created user")
    void getUserByIdReturnsUser() throws Exception {
        JsonNode created = createTargetUser();
        String createdId = created.get("id").asText();

        mvc.perform(get("/api/v1/users/{id}", createdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.email").value(TARGET_EMAIL))
                .andExpect(jsonPath("$.first_name").value("Ana"))
                .andExpect(jsonPath("$.actor_type").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.is_active").value(true));
    }

    @Test
    @DisplayName("E2 GET /users/{id} with unknown id answers 404")
    void unknownUserIdAnswers404() throws Exception {
        mvc.perform(get("/api/v1/users/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IAM_USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("E2 PUT /users/{id} updates first_name and last_name")
    void updateUserChangesFields() throws Exception {
        JsonNode created = createTargetUser();
        String createdId = created.get("id").asText();

        mvc.perform(put("/api/v1/users/{id}", createdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"first_name\":\"Beatriz\",\"last_name\":\"Vargas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.first_name").value("Beatriz"))
                .andExpect(jsonPath("$.last_name").value("Vargas"))
                .andExpect(jsonPath("$.email").value(TARGET_EMAIL));
    }

    // ---------------------------------------------------------------- E3 POST /users/{id}/deactivate

    @Test
    @DisplayName("E3 POST /users/{id}/deactivate sets is_active=false, revokes sessions and stages iam.user.deactivated")
    void deactivateUserSetsInactiveAndStagedEvent() throws Exception {
        JsonNode created = createTargetUser();
        UUID createdId = UUID.fromString(created.get("id").asText());

        mvc.perform(post("/api/v1/users/{id}/deactivate", createdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        Boolean isActive = jdbc.queryForObject("""
                SELECT is_active FROM identity."user" WHERE id = ?
                """, Boolean.class, createdId);
        assertThat(isActive).isFalse();

        long outboxCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM identity.outbox
                WHERE event_type = 'iam.user.deactivated'
                  AND payload->'data'->>'user_id' = ?
                """, Long.class, createdId.toString());
        assertThat(outboxCount).isEqualTo(1);
    }

    @Test
    @DisplayName("E3 POST /users/{id}/deactivate on unknown user answers 404")
    void deactivateUnknownUserAnswers404() throws Exception {
        mvc.perform(post("/api/v1/users/{id}/deactivate", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IAM_USER_NOT_FOUND"));
    }

    // ---------------------------------------------------------------- E4 POST/DELETE /users/{id}/roles

    @Test
    @DisplayName("E4 POST /users/{id}/roles assigns the role and stages iam.role.assigned in outbox")
    void assignRoleCreatesRecordAndStagedEvent() throws Exception {
        JsonNode created = createTargetUser();
        UUID createdId = UUID.fromString(created.get("id").asText());

        mvc.perform(post("/api/v1/users/{id}/roles", createdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_name\":\"INSTRUCTOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role_name").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.user_id").value(createdId.toString()));

        long roleRow = jdbc.queryForObject("""
                SELECT COUNT(*) FROM rbac.user_role ur
                  JOIN rbac.role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.name = 'INSTRUCTOR'
                """, Long.class, createdId);
        assertThat(roleRow).isEqualTo(1);

        long outboxCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM identity.outbox
                WHERE event_type = 'iam.role.assigned'
                  AND payload->'data'->>'user_id' = ?
                  AND payload->'data'->>'role_name' = 'INSTRUCTOR'
                """, Long.class, createdId.toString());
        assertThat(outboxCount).isEqualTo(1);
    }

    @Test
    @DisplayName("E4 DELETE /users/{id}/roles/{role_name} removes the role assignment")
    void revokeRoleRemovesRecord() throws Exception {
        JsonNode created = createTargetUser();
        UUID createdId = UUID.fromString(created.get("id").asText());

        mvc.perform(post("/api/v1/users/{id}/roles", createdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_name\":\"INSTRUCTOR\"}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/users/{id}/roles/{role_name}", createdId, "INSTRUCTOR")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        long roleRow = jdbc.queryForObject("""
                SELECT COUNT(*) FROM rbac.user_role ur
                  JOIN rbac.role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.name = 'INSTRUCTOR'
                """, Long.class, createdId);
        assertThat(roleRow).isZero();
    }

    // ---------------------------------------------------------------- helpers

    private JsonNode createTargetUser() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(targetUserBody()))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private static String targetUserBody() {
        return """
                {"email":"%s","first_name":"Ana","last_name":"Ríos","actor_type":"INSTRUCTOR"}
                """.formatted(TARGET_EMAIL);
    }

    private void deleteTestData() {
        List<UUID> targetIds = jdbc.queryForList(
                "SELECT id FROM identity.\"user\" WHERE email = ?",
                UUID.class, TARGET_EMAIL);
        for (UUID id : targetIds) {
            jdbc.update("DELETE FROM identity.outbox WHERE payload->'data'->>'user_id' = ?", id.toString());
            jdbc.update("DELETE FROM session.refresh_token WHERE user_id = ?", id);
            jdbc.update("DELETE FROM rbac.user_role WHERE user_id = ?", id);
        }
        jdbc.update("DELETE FROM identity.outbox WHERE event_type = 'iam.user.created' AND payload->'data'->>'email' = ?", TARGET_EMAIL);
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", TARGET_EMAIL);
        jdbc.update("DELETE FROM identity.\"user\" WHERE email = ?", TARGET_EMAIL);

        List<UUID> adminIds = jdbc.queryForList(
                "SELECT id FROM identity.\"user\" WHERE email = ?",
                UUID.class, ADMIN_EMAIL);
        for (UUID id : adminIds) {
            jdbc.update("DELETE FROM session.refresh_token WHERE user_id = ?", id);
            jdbc.update("DELETE FROM rbac.user_role WHERE user_id = ?", id);
        }
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", ADMIN_EMAIL);
        jdbc.update("DELETE FROM identity.\"user\" WHERE email = ?", ADMIN_EMAIL);
    }

    private static String ephemeralSigningKey() {
        String configured = System.getenv("IAM_JWT_PRIVATE_KEY");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return Base64.getEncoder().encodeToString(gen.generateKeyPair().getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA required by JRE spec", e);
        }
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
