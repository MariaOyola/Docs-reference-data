package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;

import co.sena.iam.application.port.in.AuthorizationQuery;
import co.sena.iam.application.port.in.EvaluateAuthorizationUseCase;
import co.sena.iam.domain.model.AuthorizationDecision;
import co.sena.iam.domain.model.DecisionReason;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * HU-IAM-006 E1–E3 — the feature+scope authorization decision evaluated against a real
 * PostgreSQL carrying the iam-db RBAC seeds (roles, features, role_feature scopes).
 *
 * <p>Set {@code IAM_DB_DSN}, {@code IAM_DB_USERNAME} and {@code IAM_DB_PASSWORD} to run:
 *
 * <pre>
 * IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
 * IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... ./mvnw verify
 * </pre>
 *
 * <p>Uses the seeded roles: SYSTEM_ADMIN grants SCH_CREATE at GLOBAL scope; COORDINATOR
 * grants it at TRAINING_CENTER scope. All rows written carry the {@link #MARKER_EMAIL}
 * marker and are removed by that marker alone — no truncation of shared tables.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
class AuthorizationPostgresIT {

    private static final String MARKER_EMAIL = "hu-iam-006-authz@test.sena.edu.co";
    private static final String FEATURE = "SCH_CREATE";
    private static final UUID CENTER_A = UUID.fromString("aaaa1111-1111-1111-1111-111111111111");
    private static final UUID CENTER_B = UUID.fromString("bbbb2222-2222-2222-2222-222222222222");

    @Autowired
    private EvaluateAuthorizationUseCase evaluate;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID userId;

    @DynamicPropertySource
    static void jwtSigningKey(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", AuthorizationPostgresIT::ephemeralSigningKey);
        registry.add("iam.jwt.issuer", () -> "https://iam.it.sena.local");
    }

    @BeforeEach
    void setUp() {
        deleteTestData();
        userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, 'x', 'Authz', 'Probe', 'USER', TRUE)
                """, userId, MARKER_EMAIL);
    }

    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    // ------------------------------------------------------------------ E1 permitted by feature

    @Test
    @DisplayName("E1 a user with the feature via a GLOBAL role is authorized")
    void permittedByGlobalRole() {
        assignRole("SYSTEM_ADMIN", null);

        AuthorizationDecision decision = evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_A));

        assertThat(decision.permitted()).isTrue();
        assertThat(decision.reason()).isEqualTo(DecisionReason.PERMITTED_BY_ROLE);
    }

    @Test
    @DisplayName("E1 a user without the feature is denied")
    void deniedWhenFeatureMissing() {
        // no roles assigned
        AuthorizationDecision decision = evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_A));

        assertThat(decision.permitted()).isFalse();
        assertThat(decision.reason()).isEqualTo(DecisionReason.DENIED_NO_GRANT);
    }

    // ------------------------------------------------------------------ E2 scope by training center

    @Test
    @DisplayName("E2 a coordinator is authorized inside their assigned center")
    void permittedInsideAssignedCenter() {
        assignRole("COORDINATOR", CENTER_A);

        AuthorizationDecision decision = evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_A));

        assertThat(decision.permitted()).isTrue();
        assertThat(decision.reason()).isEqualTo(DecisionReason.PERMITTED_BY_ROLE);
    }

    @Test
    @DisplayName("E2 a coordinator is denied outside their assigned center")
    void deniedOutsideAssignedCenter() {
        assignRole("COORDINATOR", CENTER_A);

        AuthorizationDecision decision = evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_B));

        assertThat(decision.permitted()).isFalse();
    }

    // ------------------------------------------------------------------ E3 overrides

    @Test
    @DisplayName("E3 an allow override grants a feature the user's roles do not")
    void allowOverrideGrantsAccess() {
        AuthorizationDecision before = evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_A));
        assertThat(before.permitted()).isFalse();

        insertOverride(FEATURE, "GLOBAL", true);

        AuthorizationDecision after = evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_A));
        assertThat(after.permitted()).isTrue();
        assertThat(after.reason()).isEqualTo(DecisionReason.PERMITTED_BY_OVERRIDE);
    }

    @Test
    @DisplayName("E3 a block override revokes a feature the user's role grants")
    void blockOverrideRevokesAccess() {
        assignRole("COORDINATOR", CENTER_A);
        assertThat(evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_A)).permitted()).isTrue();

        insertOverride(FEATURE, "TRAINING_CENTER", false);

        AuthorizationDecision after = evaluate.evaluate(new AuthorizationQuery(userId, FEATURE, CENTER_A));
        assertThat(after.permitted()).isFalse();
        assertThat(after.reason()).isEqualTo(DecisionReason.DENIED_BY_OVERRIDE);
    }

    // ------------------------------------------------------------------ helpers

    private void assignRole(String roleName, UUID trainingCenterId) {
        jdbc.update("""
                INSERT INTO rbac.user_role (user_id, role_id, training_center_id, assigned_by)
                SELECT ?, r.id, ?, ? FROM rbac.role r WHERE r.name = ?
                """, userId, trainingCenterId, userId, roleName);
    }

    private void insertOverride(String featureCode, String scopeType, boolean allowed) {
        jdbc.update("""
                INSERT INTO rbac.user_scope_override
                    (user_id, feature_id, scope_type, is_allowed, reason, granted_by)
                SELECT ?, f.id, ?, ?, 'IT', ? FROM rbac_catalog.feature f WHERE f.code = ?
                """, userId, scopeType, allowed, userId, featureCode);
    }

    private void deleteTestData() {
        for (UUID id : jdbc.queryForList(
                "SELECT id FROM identity.\"user\" WHERE email = ?", UUID.class, MARKER_EMAIL)) {
            jdbc.update("DELETE FROM rbac.user_scope_override WHERE user_id = ?", id);
            jdbc.update("DELETE FROM rbac.user_role WHERE user_id = ?", id);
            jdbc.update("DELETE FROM identity.\"user\" WHERE id = ?", id);
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
