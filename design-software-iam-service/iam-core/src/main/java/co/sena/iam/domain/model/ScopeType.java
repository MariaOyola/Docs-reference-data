package co.sena.iam.domain.model;

/**
 * Scope at which a feature is granted. Mirrors the {@code scope_type} CHECK constraint
 * shared by {@code rbac.role_feature} and {@code rbac.user_scope_override}.
 *
 * <p>HU-IAM-006 authorizes on the two center-level scopes ({@link #GLOBAL},
 * {@link #TRAINING_CENTER}); the resource-level scopes are decided downstream by the
 * owning service and are not center-authorizable here.
 */
public enum ScopeType {
    GLOBAL,
    TRAINING_CENTER,
    AREA,
    OWN_FICHAS,
    OWN_SCHEDULE,
    OWN_PROFILE,
    OWN_FICHA_AS_LEARNER;

    public static ScopeType fromCode(String code) {
        return ScopeType.valueOf(code);
    }
}
