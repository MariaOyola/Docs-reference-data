package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.domain.model.LockoutPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
 * HU-IAM-001 E1–E4 end to end against a real PostgreSQL carrying the iam-db schemas.
 *
 * <p>Environment driven — set {@code IAM_DB_DSN}, {@code IAM_DB_USERNAME} and
 * {@code IAM_DB_PASSWORD} to run it; without them the whole class is skipped, so the build
 * stays green on machines with no database. Example:
 *
 * <pre>
 * IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
 * IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... ./mvnw verify
 * </pre>
 *
 * <p>Every row it writes carries the {@link #TEST_EMAIL} marker and is deleted by that marker
 * alone — no blanket truncation of shared tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class LoginPostgresIT {

    /** Marker that scopes every fixture row this test creates, and its cleanup. */
    private static final String TEST_EMAIL = "hu-iam-001-it@test.sena.edu.co";
    private static final String PASSWORD = "Integr4tion!Test";
    private static final String WRONG_PASSWORD = "definitely-not-it";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private UUID userId;

    /**
     * The signing key is ephemeral unless the environment supplies one: it is generated per
     * run, never a checked-in secret.
     */
    @DynamicPropertySource
    static void jwtSigningKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", LoginPostgresIT::signingKeyFromEnvOrGenerated);
        registry.add("iam.jwt.issuer", () -> envOr("IAM_JWT_ISSUER", "https://iam.it.sena.local"));
    }

    private static String signingKeyFromEnvOrGenerated() {
        String configured = System.getenv("IAM_JWT_PRIVATE_KEY");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is required by the JRE spec", e);
        }
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @BeforeEach
    void createTestUser() {
        deleteTestData();
        userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, ?, 'Ana', 'Ríos', 'INSTRUCTOR', TRUE)
                """, userId, TEST_EMAIL, new BCryptPasswordEncoder().encode(PASSWORD));
        jdbc.update("""
                INSERT INTO rbac.user_role (user_id, role_id, assigned_by)
                SELECT ?, r.id, ? FROM rbac.role r WHERE r.name = 'INSTRUCTOR'
                """, userId, userId);
    }

    @AfterEach
    void cleanUp() {
        deleteTestData();
    }

    /** Targeted by the test marker only. */
    private void deleteTestData() {
        jdbc.update("""
                DELETE FROM identity.outbox
                 WHERE payload->'data'->>'user_id' IN (
                     SELECT id::text FROM identity."user" WHERE email = ?)
                """, TEST_EMAIL);
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", TEST_EMAIL);
        jdbc.update("DELETE FROM identity.\"user\" WHERE email = ?", TEST_EMAIL);
    }

    // ---------------------------------------------------------------- E1

    @Test
    @DisplayName("E1 valid credentials return both tokens, persist the session and audit the success")
    void loginSucceeds() throws Exception {
        MvcResult result = login(PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.roles[0]").value("INSTRUCTOR"))
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("access_token").asText()).contains(".");
        assertThat(body.get("refresh_token").asText()).isNotBlank();
        assertThat(body.get("user").get("features")).isNotEmpty();

        assertThat(auditOutcomes()).containsExactly("SUCCESS");
        assertThat(refreshTokenRows()).hasSize(1);
        Map<String, Object> session = refreshTokenRows().get(0);
        assertThat(session.get("token_hash")).isNotNull();
        assertThat(session.get("token_hash").toString())
                .as("only the digest is stored")
                .isNotEqualTo(body.get("refresh_token").asText());
        assertThat(timestamp(session.get("expires_at")))
                .isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusDays(6));

        Map<String, Object> user = userRow();
        assertThat(((Number) user.get("failed_attempts")).intValue()).isZero();
        assertThat(user.get("locked_until")).isNull();
        assertThat(user.get("last_login_at")).isNotNull();

        assertThat(stagedOutboxEventTypes())
                .as("a successful login always stages iam.session.started, independent of the relay")
                .contains("iam.session.started");
    }

    @Test
    @DisplayName("E1 a successful login clears a counter left behind by earlier failures")
    void successResetsTheCounter() throws Exception {
        login(WRONG_PASSWORD).andExpect(status().isUnauthorized());
        login(WRONG_PASSWORD).andExpect(status().isUnauthorized());
        assertThat(failedAttempts()).isEqualTo(2);

        login(PASSWORD).andExpect(status().isOk());

        assertThat(failedAttempts()).isZero();
        assertThat(auditOutcomes()).containsExactly("INVALID_PASSWORD", "INVALID_PASSWORD", "SUCCESS");
    }

    // ---------------------------------------------------------------- E2

    @Test
    @DisplayName("E2 a wrong password answers 401, is audited and increments the counter")
    void loginWithWrongPasswordIsRefusedAndCounted() throws Exception {
        login(WRONG_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_INVALID_CREDENTIALS"));

        assertThat(auditOutcomes()).containsExactly("INVALID_PASSWORD");
        assertThat(failedAttempts()).isEqualTo(1);
        assertThat(refreshTokenRows()).as("no session for a refused login").isEmpty();
    }

    @Test
    @DisplayName("E2 an unknown email answers 401 and is audited as USER_NOT_FOUND")
    void loginWithUnknownEmailIsAudited() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ghost-" + TEST_EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_INVALID_CREDENTIALS"));

        List<String> outcomes = jdbc.queryForList(
                "SELECT outcome FROM identity_audit.audit_login WHERE email_attempted = ?",
                String.class, "ghost-" + TEST_EMAIL);
        assertThat(outcomes).containsExactly("USER_NOT_FOUND");
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", "ghost-" + TEST_EMAIL);
    }

    // ---------------------------------------------------------------- E3

    @Test
    @DisplayName("E3 five failures lock the account for 15 minutes and the next login answers 423")
    void fiveFailuresLockTheAccount() throws Exception {
        for (int attempt = 0; attempt < LockoutPolicy.SHORT_LOCK_THRESHOLD; attempt++) {
            login(WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }

        Map<String, Object> user = userRow();
        assertThat(((Number) user.get("failed_attempts")).intValue()).isEqualTo(5);
        OffsetDateTime lockedUntil = timestamp(user.get("locked_until"));
        assertThat(lockedUntil).isNotNull();
        assertThat(lockedUntil).isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(14));
        assertThat(lockedUntil).isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(16));

        // Even the right password is refused while the lock stands.
        login(PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error.code").value("IAM_ACCOUNT_LOCKED"));

        assertThat(auditOutcomes()).endsWith("ACCOUNT_LOCKED");
        assertThat(failedAttempts()).as("a blocked attempt does not count").isEqualTo(5);
    }

    @Test
    @DisplayName("E3 the tenth failure escalates the lock to 24 hours")
    void tenthFailureLocksForADay() throws Exception {
        jdbc.update("UPDATE identity.\"user\" SET failed_attempts = 9 WHERE id = ?", userId);

        login(WRONG_PASSWORD).andExpect(status().isUnauthorized());

        Map<String, Object> user = userRow();
        assertThat(((Number) user.get("failed_attempts")).intValue()).isEqualTo(10);
        assertThat(timestamp(user.get("locked_until")))
                .isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusHours(23));
    }

    @Test
    @DisplayName("E3 once the lock has elapsed the same credentials work again")
    void loginWorksAfterTheLockElapsed() throws Exception {
        jdbc.update("""
                UPDATE identity."user"
                   SET failed_attempts = 5, locked_until = now() - INTERVAL '1 minute'
                 WHERE id = ?
                """, userId);

        login(PASSWORD).andExpect(status().isOk());

        assertThat(failedAttempts()).isZero();
        assertThat(userRow().get("locked_until")).isNull();
    }

    // ---------------------------------------------------------------- E4

    @Test
    @DisplayName("E4 GET /auth/me with the issued token returns the identity, roles and modules")
    void meReturnsTheAuthenticatedIdentity() throws Exception {
        MvcResult loginResult = login(PASSWORD).andExpect(status().isOk()).andReturn();
        String accessToken = json.readTree(loginResult.getResponse().getContentAsString())
                .get("access_token").asText();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.full_name").value("Ana Ríos"))
                .andExpect(jsonPath("$.actor_type").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.roles[0].name").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.features").isNotEmpty())
                .andExpect(jsonPath("$.modules").isNotEmpty());
    }

    @Test
    @DisplayName("E4 GET /auth/me refuses a token this service did not sign")
    void meRefusesAForgedToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    // ---------------------------------------------------------------- helpers

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(LoginPostgresIT.TEST_EMAIL, password)));
    }

    private static String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private List<String> auditOutcomes() {
        return jdbc.queryForList("""
                SELECT outcome FROM identity_audit.audit_login
                 WHERE email_attempted = ? ORDER BY attempted_at, outcome
                """, String.class, TEST_EMAIL);
    }

    private List<Map<String, Object>> refreshTokenRows() {
        return jdbc.queryForList("SELECT * FROM session.refresh_token WHERE user_id = ?", userId);
    }

    private List<String> stagedOutboxEventTypes() {
        return jdbc.queryForList(
                "SELECT event_type FROM identity.outbox WHERE payload->'data'->>'user_id' = ?",
                String.class, userId.toString());
    }

    private Map<String, Object> userRow() {
        return jdbc.queryForMap("SELECT * FROM identity.\"user\" WHERE id = ?", userId);
    }

    private int failedAttempts() {
        return ((Number) userRow().get("failed_attempts")).intValue();
    }

    /** {@code timestamptz} comes back as {@link java.sql.Timestamp} through JdbcTemplate. */
    private static OffsetDateTime timestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        return ((java.sql.Timestamp) value).toInstant().atOffset(ZoneOffset.UTC);
    }
}
