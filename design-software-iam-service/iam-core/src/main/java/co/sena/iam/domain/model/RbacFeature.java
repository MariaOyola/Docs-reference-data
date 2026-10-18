package co.sena.iam.domain.model;

import java.util.Objects;

/**
 * Feature entry within a catalog module.
 * Mirrors rbac_catalog.feature; scope_type is resolved from rbac.role_feature
 * (most permissive assignment across all roles, GLOBAL fallback when unassigned).
 */
public record RbacFeature(
        String code,
        String name,
        String description,
        String actionLevel,
        String scopeType) {

    public RbacFeature {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(actionLevel, "actionLevel");
        Objects.requireNonNull(scopeType, "scopeType");
    }
}
