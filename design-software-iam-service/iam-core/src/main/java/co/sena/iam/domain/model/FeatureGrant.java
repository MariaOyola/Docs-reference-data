package co.sena.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A feature a user reaches through one of their role assignments, with the scope that
 * assignment applies. Derived from {@code rbac.user_role} joined with
 * {@code rbac.role_feature} and {@code rbac_catalog.feature}.
 *
 * @param trainingCenterId the center the assignment is scoped to; {@code null} for a
 *                         global (center-less) assignment
 */
public record FeatureGrant(String featureCode, ScopeType scopeType, UUID trainingCenterId) {

    public FeatureGrant {
        Objects.requireNonNull(featureCode, "featureCode");
        Objects.requireNonNull(scopeType, "scopeType");
    }
}
