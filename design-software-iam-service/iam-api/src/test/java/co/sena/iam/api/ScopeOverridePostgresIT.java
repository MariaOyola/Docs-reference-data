package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * HU-IAM-006 E3 end to end against a real PostgreSQL: an administrator grants, lists and
 * revokes an individual scope override over {@code /users/{id}/scope-overrides}.
 *
 * <p>Set {@code IAM_DB_DSN}, {@code IAM_DB_USERNAME} and {@code IAM_DB_PASSWORD} to run.
 * All rows written carry the {@link #ADMIN_EMAIL} / {@link #TARGET_EMAIL} markers and are
 * removed by those markers alone — no truncation of shared tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class ScopeOverridePostgresIT {

    private static final String ADMIN_EMAIL = "hu-iam-006-admin@test.sena.edu.co";
    private static final String TARGET_EMAIL = "hu-iam-006-target@test.sena.edu.co";
    private static final String PASSWORD = "Integr4tion!Test";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    private UUID adminId;
    private UUID targetId;
    private String token;

    @DynamicPropertySource
    static void jwtSigningKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", ScopeOverridePostgresIT::ephemeralSigningKey);
        registry.add("iam.jwt.issuer", () -> "https://iam.it.sena.local");
    }

    @BeforeEach
    void setUp() throws Exception {
        deleteTestData();
        adminId = insertUser(ADMIN_EMAIL);
        targetId = insertUser(TARGET_EMAIL);
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

    @Test
    @DisplayName("E3 POST creates the override, stages iam.scope_override.granted, and GET lists it")
    void grantAndList() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/users/{id}/scope-overrides", targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feature_code":"SCH_CREATE","scope_type":"GLOBAL",
                                 "is_allowed":true,"reason":"cobertura temporal"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.feature_code").value("SCH_CREATE"))
                .andExpect(jsonPath("$.is_allowed").value(true))
                .andExpect(jsonPath("$.granted_by").value(adminId.toString()))
                .andReturn();

        UUID overrideId = UUID.fromString(
                json.readTree(created.getResponse().getContentAsString()).get("id").asText());

        long outbox = jdbc.queryForObject("""
                SELECT COUNT(*) FROM identity.outbox
                WHERE event_type = 'iam.scope_override.granted'
                  AND payload->'data'->>'override_id' = ?
                """, Long.class, overrideId.toString());
        assertThat(outbox).isEqualTo(1);

        mvc.perform(get("/api/v1/users/{id}/scope-overrides", targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(overrideId.toString()))
                .andExpect(jsonPath("$[0].feature_code").value("SCH_CREATE"));
    }

    @Test
    @DisplayName("E3 DELETE removes the override and stages iam.scope_override.revoked")
    void revoke() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/users/{id}/scope-overrides", targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feature_code":"SCH_CREATE","scope_type":"GLOBAL",
                                 "is_allowed":false,"reason":"bloqueo"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID overrideId = UUID.fromString(
                json.readTree(created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(delete("/api/v1/users/{id}/scope-overrides/{overrideId}", targetId, overrideId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        long rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rbac.user_scope_override WHERE id = ?", Long.class, overrideId);
        assertThat(rows).isZero();

        long revoked = jdbc.queryForObject("""
                SELECT COUNT(*) FROM identity.outbox
                WHERE event_type = 'iam.scope_override.revoked'
                  AND payload->'data'->>'override_id' = ?
                """, Long.class, overrideId.toString());
        assertThat(revoked).isEqualTo(1);
    }

    @Test
    @DisplayName("E3 POST with an unknown feature answers 422")
    void unknownFeatureAnswers422() throws Exception {
        mvc.perform(post("/api/v1/users/{id}/scope-overrides", targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feature_code":"NOPE_DOES_NOT_EXIST","scope_type":"GLOBAL",
                                 "is_allowed":true,"reason":"x"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("IAM_FEATURE_NOT_FOUND"));
    }

    @Test
    @DisplayName("E3 DELETE of an unknown override answers 404")
    void unknownOverrideAnswers404() throws Exception {
        mvc.perform(delete("/api/v1/users/{id}/scope-overrides/{overrideId}", targetId, UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IAM_SCOPE_OVERRIDE_NOT_FOUND"));
    }

    @Test
    @DisplayName("E3 POST without a token answers 401")
    void withoutTokenAnswers401() throws Exception {
        mvc.perform(post("/api/v1/users/{id}/scope-overrides", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feature_code":"SCH_CREATE","scope_type":"GLOBAL",
                                 "is_allowed":true,"reason":"x"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ helpers

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, ?, 'It', 'User', 'USER', TRUE)
                """, id, email, new BCryptPasswordEncoder().encode(PASSWORD));
        return id;
    }

    private void deleteTestData() {
        for (String email : List.of(ADMIN_EMAIL, TARGET_EMAIL)) {
            for (UUID id : jdbc.queryForList(
                    "SELECT id FROM identity.\"user\" WHERE email = ?", UUID.class, email)) {
                jdbc.update("DELETE FROM identity.outbox WHERE payload->'data'->>'user_id' = ?", id.toString());
                jdbc.update("DELETE FROM rbac.user_scope_override WHERE user_id = ? OR granted_by = ?", id, id);
                jdbc.update("DELETE FROM rbac.user_role WHERE user_id = ?", id);
                jdbc.update("DELETE FROM session.refresh_token WHERE user_id = ?", id);
                jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", email);
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
