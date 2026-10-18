package co.sena.iam.domain.model;

/** Why an {@link AuthorizationDecision} came out the way it did. */
public enum DecisionReason {

    /** A role grants the feature at a scope that covers the target. */
    PERMITTED_BY_ROLE,

    /** A personal allow override grants the feature exceptionally. */
    PERMITTED_BY_OVERRIDE,

    /** A personal block override revokes the feature (deny wins over any grant). */
    DENIED_BY_OVERRIDE,

    /** No role or override grants the feature at the target scope. */
    DENIED_NO_GRANT
}
