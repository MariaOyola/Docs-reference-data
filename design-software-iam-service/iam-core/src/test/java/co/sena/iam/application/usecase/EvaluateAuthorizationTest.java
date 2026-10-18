package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import co.sena.iam.application.port.in.AuthorizationQuery;
import co.sena.iam.application.port.out.AuthorizationProfilePort;
import co.sena.iam.domain.model.AuthorizationProfile;
import co.sena.iam.domain.model.DecisionReason;
import co.sena.iam.domain.model.FeatureGrant;
import co.sena.iam.domain.model.ScopeType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-006 — the use case loads the user's profile and applies the policy at "now". */
class EvaluateAuthorizationTest {

    private static final String FEATURE = "SCH_CREATE";
    private static final UUID USER = UUID.randomUUID();
    private static final UUID CENTER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CENTER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("permits when the loaded profile's role grant covers the target center")
    void permitsWhenCenterScopedGrantMatches() {
        var useCase = new EvaluateAuthorization(portFor(Map.of(USER, new AuthorizationProfile(
                List.of(new FeatureGrant(FEATURE, ScopeType.TRAINING_CENTER, CENTER_A)),
                List.of()))), clock);

        var decision = useCase.evaluate(new AuthorizationQuery(USER, FEATURE, CENTER_A));

        assertThat(decision.permitted()).isTrue();
        assertThat(decision.reason()).isEqualTo(DecisionReason.PERMITTED_BY_ROLE);
    }

    @Test
    @DisplayName("denies when the operation targets a center outside the user's scope")
    void deniesOutsideCenter() {
        var useCase = new EvaluateAuthorization(portFor(Map.of(USER, new AuthorizationProfile(
                List.of(new FeatureGrant(FEATURE, ScopeType.TRAINING_CENTER, CENTER_A)),
                List.of()))), clock);

        var decision = useCase.evaluate(new AuthorizationQuery(USER, FEATURE, CENTER_B));

        assertThat(decision.permitted()).isFalse();
    }

    @Test
    @DisplayName("an unknown user carries no grants and is denied")
    void unknownUserDenied() {
        var useCase = new EvaluateAuthorization(portFor(Map.of()), clock);

        var decision = useCase.evaluate(new AuthorizationQuery(UUID.randomUUID(), FEATURE, CENTER_A));

        assertThat(decision.permitted()).isFalse();
        assertThat(decision.reason()).isEqualTo(DecisionReason.DENIED_NO_GRANT);
    }

    private static AuthorizationProfilePort portFor(Map<UUID, AuthorizationProfile> profiles) {
        return userId -> profiles.getOrDefault(userId, AuthorizationProfile.empty());
    }
}
