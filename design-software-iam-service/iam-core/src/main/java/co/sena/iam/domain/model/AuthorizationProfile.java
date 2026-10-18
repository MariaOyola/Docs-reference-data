package co.sena.iam.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Everything the authorization policy needs to decide for one user at a point in time:
 * the feature grants their roles resolve to, plus their personal scope overrides.
 */
public record AuthorizationProfile(List<FeatureGrant> grants, List<ScopeOverrideRecord> overrides) {

    public AuthorizationProfile {
        grants = List.copyOf(Objects.requireNonNull(grants, "grants"));
        overrides = List.copyOf(Objects.requireNonNull(overrides, "overrides"));
    }

    public static AuthorizationProfile empty() {
        return new AuthorizationProfile(List.of(), List.of());
    }

    /** The training centers the user reaches through any of their role grants. */
    public Set<UUID> reachableCenters() {
        return grants.stream()
                .map(FeatureGrant::trainingCenterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
