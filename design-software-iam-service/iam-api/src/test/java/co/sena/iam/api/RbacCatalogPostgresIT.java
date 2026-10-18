package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
 * HU-IAM-004 E1–E3 end to end against a real PostgreSQL carrying the iam-db schemas and seeds.
 *
 * <p>Set {@code IAM_DB_DSN}, {@code IAM_DB_USERNAME} and {@code IAM_DB_PASSWORD} to run:
 *
 * <pre>
 * IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
 * IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... ./mvnw verify
 * </pre>
 *
 * <p>Reads are against the seed data; the only writes are the per-test {@link #TEST_EMAIL}
 * admin fixture created by {@link #loginAsAdmin()} and removed by {@link #tearDown()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class RbacCatalogPostgresIT {

    private static final String TEST_EMAIL = "hu-iam-004-it@test.sena.edu.co";
    private static final String PASSWORD = "Integr4tion!Test";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    @DynamicPropertySource
    static void jwtSigningKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", RbacCatalogPostgresIT::ephemeralSigningKey);
        registry.add("iam.jwt.issuer", () -> envOr("IAM_JWT_ISSUER", "https://iam.it.sena.local"));
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

    // ---------------------------------------------------------------- helpers

    /**
     * Inserts a SYSTEM_ADMIN test user and returns a valid access token.
     *
     * <p>The fixture is deliberately left in place for the caller to use the token against
     * — it is removed by {@link #tearDown()}, not here, since deleting it right after login
     * would invalidate the token for every request the test still has to make.
     */
    private String loginAsAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, ?, 'It', 'Admin', 'USER', TRUE)
                """, userId, TEST_EMAIL, new BCryptPasswordEncoder().encode(PASSWORD));
        jdbc.update("""
                INSERT INTO rbac.user_role (user_id, role_id, assigned_by)
                SELECT ?, r.id, ? FROM rbac.role r WHERE r.name = 'SYSTEM_ADMIN'
                """, userId, userId);
        MvcResult result = mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString())
                .get("access_token").asText();
    }

    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    private void deleteTestData() {
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted = ?", TEST_EMAIL);
        jdbc.update("DELETE FROM identity.\"user\" WHERE email = ?", TEST_EMAIL);
    }

    // ---------------------------------------------------------------- E1 GET /modules

    @Test
    @DisplayName("E1 GET /modules returns seeded modules with nested features")
    void listModulesReturnsSeedData() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mvc.perform(get("/api/v1/modules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThanOrEqualTo(1);

        JsonNode firstModule = body.get(0);
        assertThat(firstModule.get("code").asText()).isNotBlank();
        assertThat(firstModule.get("name").asText()).isNotBlank();
        assertThat(firstModule.get("features").isArray()).isTrue();
        assertThat(firstModule.get("features").size()).isGreaterThan(0);

        JsonNode firstFeature = firstModule.get("features").get(0);
        assertThat(firstFeature.get("code").asText()).isNotBlank();
        assertThat(firstFeature.get("action_level").asText()).isNotBlank();
        assertThat(firstFeature.get("scope_type").asText()).isNotBlank();
    }

    @Test
    @DisplayName("E1 GET /modules contains MOD_IDENTITY with IDENTITY_USER_VIEW feature")
    void listModulesContainsIdentityModule() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mvc.perform(get("/api/v1/modules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        boolean foundIdentity = false;
        for (JsonNode mod : body) {
            if ("MOD_IDENTITY".equals(mod.get("code").asText())) {
                foundIdentity = true;
                boolean foundFeature = false;
                for (JsonNode f : mod.get("features")) {
                    if ("IDENTITY_USER_VIEW".equals(f.get("code").asText())) {
                        foundFeature = true;
                        break;
                    }
                }
                assertThat(foundFeature).as("IDENTITY_USER_VIEW in MOD_IDENTITY").isTrue();
                break;
            }
        }
        assertThat(foundIdentity).as("MOD_IDENTITY in catalog").isTrue();
    }

    @Test
    @DisplayName("E1 GET /modules without token answers 401")
    void listModulesWithoutTokenIsRefused() throws Exception {
        mvc.perform(get("/api/v1/modules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    // ---------------------------------------------------------------- E1 GET /roles

    @Test
    @DisplayName("E1 GET /roles returns seeded roles")
    void listRolesReturnsSeedRoles() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThanOrEqualTo(7);

        boolean hasSysAdmin = false;
        for (JsonNode role : body) {
            if ("SYSTEM_ADMIN".equals(role.get("name").asText())) {
                hasSysAdmin = true;
                assertThat(role.get("is_system_role").asBoolean()).isTrue();
                assertThat(role.get("id").asText()).isNotBlank();
                assertThat(role.get("display_name").asText()).isNotBlank();
                break;
            }
        }
        assertThat(hasSysAdmin).as("SYSTEM_ADMIN role in catalog").isTrue();
    }

    @Test
    @DisplayName("E2 GET /roles lists roles independent of training_center scope — scope lives in role_feature")
    void rolesAreListedWithoutCenterScope() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        for (JsonNode role : body) {
            assertThat(role.get("id")).isNotNull();
            assertThat(role.get("name")).isNotNull();
        }
    }

    @Test
    @DisplayName("E1 GET /roles without token answers 401")
    void listRolesWithoutTokenIsRefused() throws Exception {
        mvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- E1/E3 GET /roles/{id}/features

    @Test
    @DisplayName("E1/E3 GET /roles/{id}/features returns features with scope_type for SYSTEM_ADMIN")
    void sysAdminFeaturesHaveGlobalScope() throws Exception {
        String token = loginAsAdmin();

        UUID sysAdminId = jdbc.queryForObject(
                "SELECT id FROM rbac.role WHERE name = 'SYSTEM_ADMIN'", UUID.class);

        MvcResult result = mvc.perform(get("/api/v1/roles/{id}/features", sysAdminId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThan(0);

        for (JsonNode entry : body) {
            assertThat(entry.get("feature_code").asText()).isNotBlank();
            assertThat(entry.get("scope_type").asText()).isNotBlank();
        }

        boolean hasGlobal = false;
        for (JsonNode entry : body) {
            if ("GLOBAL".equals(entry.get("scope_type").asText())) {
                hasGlobal = true;
                break;
            }
        }
        assertThat(hasGlobal).as("SYSTEM_ADMIN should have at least one GLOBAL feature").isTrue();
    }

    @Test
    @DisplayName("E2 CENTER_DIRECTOR has TRAINING_CENTER scope on some features")
    void centerDirectorHasTrainingCenterScope() throws Exception {
        String token = loginAsAdmin();

        UUID centerId = jdbc.queryForObject(
                "SELECT id FROM rbac.role WHERE name = 'CENTER_DIRECTOR'", UUID.class);

        MvcResult result = mvc.perform(get("/api/v1/roles/{id}/features", centerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        boolean hasTrainingCenter = false;
        for (JsonNode entry : body) {
            if ("TRAINING_CENTER".equals(entry.get("scope_type").asText())) {
                hasTrainingCenter = true;
                break;
            }
        }
        assertThat(hasTrainingCenter)
                .as("CENTER_DIRECTOR should have at least one TRAINING_CENTER-scoped feature").isTrue();
    }

    @Test
    @DisplayName("E1 unknown role id answers 404")
    void unknownRoleAnswers404() throws Exception {
        String token = loginAsAdmin();
        UUID fakeId = UUID.randomUUID();

        mvc.perform(get("/api/v1/roles/{id}/features", fakeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("E1 GET /roles/{id}/features without token answers 401")
    void roleFeaturesWithoutTokenIsRefused() throws Exception {
        mvc.perform(get("/api/v1/roles/{id}/features", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
