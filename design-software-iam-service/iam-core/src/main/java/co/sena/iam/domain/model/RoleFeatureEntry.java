package co.sena.iam.domain.model;

import java.util.Objects;

/** Feature granted by a role, with the scope that role applies. Mirrors rbac.role_feature + rbac_catalog.feature. */
public record RoleFeatureEntry(
        String featureCode,
        String featureName,
        String actionLevel,
        String scopeType) {

    public RoleFeatureEntry {
        Objects.requireNonNull(featureCode, "featureCode");
        Objects.requireNonNull(featureName, "featureName");
        Objects.requireNonNull(actionLevel, "actionLevel");
        Objects.requireNonNull(scopeType, "scopeType");
    }
}
