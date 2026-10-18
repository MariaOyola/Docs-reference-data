package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
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
 * HU-IAM-002 E1–E3 end to end against a real PostgreSQL carrying the iam-db schemas.
 *
 * <p>Environment driven — set {@code IAM_DB_DSN}, {@code IAM_DB_USERNAME} and
 * {@code IAM_DB_PASSWORD} to run it; without them the whole class is skipped.
 *
 * <pre>
 * IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
 * IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... ./mvnw verify
 * </pre>
 *
 * <p>Every row written carries the {@link #TEST_EMAIL} marker and is cleaned up afterward.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class SessionPostgresIT {

    private static final String TEST_EMAIL = "hu-iam-002-it@test.sena.edu.co";
    private static final String PASSWORD = "Integr4tion!Test";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private UUID userId;

    @DynamicPropertySource
    static void jwtSigningKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", SessionPostgresIT::generatedOrEnvKey);
        registry.add("iam.jwt.issuer", () -> envOr("IAM_JWT_ISSUER", "https://iam.it.sena.local"));
    }

    private static String generatedOrEnvKey() {
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
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    @BeforeEach
    void createTestUser() {
        deleteTestData();
        userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, ?, 'Iti', 'Tester', 'INSTRUCTOR', TRUE)
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

    private void deleteTestData() {
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", TEST_EMAIL);
        jdbc.update("DELETE FROM identity.\"user\" WHERE email = ?", TEST_EMAIL);
    }

    // ---------------------------------------------------------------- E1

    @Test
    @DisplayName("E1 POST /auth/refresh with valid refresh token returns 200 with a new access_token")
    void refreshWithValidTokenReturns200() throws Exception {
        Tokens tokens = loginAndGetTokens();

        MvcResult result = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"" + tokens.refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("access_token").asText())
                .as("new access token must differ from the original")
                .isNotEqualTo(tokens.accessToken);
        assertThat(body.get("refresh_token").asText())
                .as("refresh token is not rotated")
                .isEqualTo(tokens.refreshToken);
    }

    @Test
    @DisplayName("E1 POST /auth/refresh with unknown or wrong token returns 401 IAM_TOKEN_REVOKED")
    void refreshWithUnknownTokenReturns401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"totally-made-up-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_TOKEN_REVOKED"));
    }

    // ---------------------------------------------------------------- E2

    @Test
    @DisplayName("E2 POST /auth/logout revokes the session; reusing the refresh token answers 401")
    void logoutRevokesAndPreventsRefresh() throws Exception {
        Tokens tokens = loginAndGetTokens();

        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken))
                .andExpect(status().isNoContent());

        List<Map<String, Object>> sessions = refreshTokenRows();
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).get("is_revoked")).isEqualTo(Boolean.TRUE);

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"" + tokens.refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_TOKEN_REVOKED"));
    }

    // ---------------------------------------------------------------- E3

    @Test
    @DisplayName("E3 GET /users/{id}/sessions lists the active session created by login")
    void listSessionsShowsActiveSession() throws Exception {
        Tokens tokens = loginAndGetTokens();

        mvc.perform(get("/api/v1/users/{id}/sessions", userId)
                        .header("Authorization", "Bearer " + tokens.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].is_revoked").value(false));
    }

    @Test
    @DisplayName("E3 DELETE /users/{id}/sessions/{session_id} revokes the specific session")
    void revokeSessionMarksItRevoked() throws Exception {
        Tokens tokens = loginAndGetTokens();

        List<Map<String, Object>> sessions = refreshTokenRows();
        assertThat(sessions).hasSize(1);
        UUID sessionId = (UUID) sessions.get(0).get("id");

        mvc.perform(delete("/api/v1/users/{id}/sessions/{session_id}", userId, sessionId)
                        .header("Authorization", "Bearer " + tokens.accessToken))
                .andExpect(status().isNoContent());

        List<Map<String, Object>> after = refreshTokenRows();
        assertThat(after).hasSize(1);
        assertThat(after.get(0).get("is_revoked")).isEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("E3 DELETE /users/{id}/sessions/{session_id} for unknown session returns 404")
    void revokeUnknownSessionReturns404() throws Exception {
        Tokens tokens = loginAndGetTokens();

        mvc.perform(delete("/api/v1/users/{id}/sessions/{session_id}",
                        userId, UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokens.accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IAM_SESSION_NOT_FOUND"));
    }

    // ---------------------------------------------------------------- helpers

    private record Tokens(String accessToken, String refreshToken) {}

    private Tokens loginAndGetTokens() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return new Tokens(body.get("access_token").asText(), body.get("refresh_token").asText());
    }

    private List<Map<String, Object>> refreshTokenRows() {
        return jdbc.queryForList("SELECT * FROM session.refresh_token WHERE user_id = ?", userId);
    }
}
