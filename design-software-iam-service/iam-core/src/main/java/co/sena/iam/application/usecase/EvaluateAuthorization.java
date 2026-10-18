package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.AuthorizationQuery;
import co.sena.iam.application.port.in.EvaluateAuthorizationUseCase;
import co.sena.iam.application.port.out.AuthorizationProfilePort;
import co.sena.iam.domain.model.AuthorizationDecision;
import co.sena.iam.domain.model.AuthorizationProfile;
import co.sena.iam.domain.service.AuthorizationPolicy;
import java.time.Clock;
import java.util.Objects;

/** HU-IAM-006 — resolves the user's authorization profile and applies {@link AuthorizationPolicy}. */
public final class EvaluateAuthorization implements EvaluateAuthorizationUseCase {

    private final AuthorizationProfilePort profiles;
    private final AuthorizationPolicy policy;
    private final Clock clock;

    public EvaluateAuthorization(AuthorizationProfilePort profiles, Clock clock) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.policy = new AuthorizationPolicy();
    }

    @Override
    public AuthorizationDecision evaluate(AuthorizationQuery query) {
        Objects.requireNonNull(query, "query");
        AuthorizationProfile profile = profiles.loadProfile(query.userId());
        return policy.decide(profile, query.featureCode(), query.trainingCenterId(), clock.instant());
    }
}
