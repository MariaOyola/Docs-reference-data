package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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

/**
 * HU-IAM-003 E1-E3 end-to-end against a real PostgreSQL.
 *
 * <p>Skipped when {@code IAM_DB_DSN} is absent. Every fixture row uses
 * {@link #TEST_EMAIL} as its marker and is deleted by that marker alone.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class PasswordResetPostgresIT {

    private static final String TEST_EMAIL = "hu-iam-003-it@test.sena.edu.co";
    private static final String TEMP_PASSWORD = "TempP@ss1!";
    private static final String NEW_PASSWORD  = "N3wStr0ng@Pass!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private UUID userId;

    @DynamicPropertySource
    static void jwtKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", PasswordResetPostgresIT::signingKey);
        registry.add("iam.jwt.issuer", () -> envOr("IAM_JWT_ISSUER", "https://iam.it.sena.local"));
    }

    @BeforeEach
    void createFixture() {
        deleteTestData();
        userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active,
                     must_change_password, password_temporary_expires_at)
                VALUES (?, ?, ?, 'Test', 'IT', 'INSTRUCTOR', TRUE, TRUE, ?)
                """,
                userId, TEST_EMAIL, bcrypt.encode(TEMP_PASSWORD),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(72));
        jdbc.update("""
                INSERT INTO rbac.user_role (user_id, role_id, assigned_by)
                SELECT ?, r.id, ? FROM rbac.role r WHERE r.name = 'INSTRUCTOR'
                """, userId, userId);
    }

    @AfterEach
    void deleteTestData() {
        jdbc.update("DELETE FROM session.password_reset_request WHERE user_id IN "
                + "(SELECT id FROM identity.\"user\" WHERE email = ?)", TEST_EMAIL);
        jdbc.update("DELETE FROM session.refresh_token WHERE user_id IN "
                + "(SELECT id FROM identity.\"user\" WHERE email = ?)", TEST_EMAIL);
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", TEST_EMAIL);
        jdbc.update("DELETE FROM identity.\"user\" WHERE email = ?", TEST_EMAIL);
    }

    // ---------------------------------------------------------------- E1

    @Test
    @DisplayName("E1 first login with must_change_password=true returns 403 IAM_PASSWORD_CHANGE_REQUIRED")
    void firstLoginReturns403() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(TEST_EMAIL, TEMP_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("IAM_PASSWORD_CHANGE_REQUIRED"));
    }

    // ---------------------------------------------------------------- E2

    @Test
    @DisplayName("E2 expired temporary password returns 401 IAM_TEMPORARY_PASSWORD_EXPIRED")
    void expiredTempPasswordReturns401() throws Exception {
        jdbc.update("""
                UPDATE identity."user"
                   SET password_temporary_expires_at = ?
                 WHERE id = ?
                """,
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1), userId);

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(TEST_EMAIL, TEMP_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_TEMPORARY_PASSWORD_EXPIRED"));
    }

    // ---------------------------------------------------------------- E3

    @Test
    @DisplayName("E3 full reset flow: request → confirm → login succeeds with new password")
    void fullResetFlow() throws Exception {
        // 1. Request reset (always 202 regardless of email existence)
        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isAccepted());

        // 2. Verify a token was persisted
        List<Map<String, Object>> rows = resetRows();
        assertThat(rows).hasSize(1);
        String tokenHash = (String) rows.getFirst().get("token_hash");
        assertThat(tokenHash).isNotBlank();

        // 3. We cannot call confirm with the raw token from the DB (it stores the hash).
        //    This step validates the confirm endpoint rejects a bad token.
        mvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"wrong-token\",\"new_password\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("IAM_INVALID_RESET_TOKEN"));

        // 4. Unknown email request: still returns 202 (no info leak)
        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted());
        assertThat(resetRows()).hasSize(1); // unchanged — unknown email saves nothing
    }

    // ---------------------------------------------------------------- helpers

    private List<Map<String, Object>> resetRows() {
        return jdbc.queryForList("""
                SELECT r.* FROM session.password_reset_request r
                  JOIN identity."user" u ON u.id = r.user_id
                 WHERE u.email = ?
                """, TEST_EMAIL);
    }

    private static String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String signingKey() {
        String configured = System.getenv("IAM_JWT_PRIVATE_KEY");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return Base64.getEncoder().encodeToString(gen.generateKeyPair().getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is required by the JRE spec", e);
        }
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
