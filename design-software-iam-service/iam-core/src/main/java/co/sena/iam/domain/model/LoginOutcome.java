package co.sena.iam.domain.model;

/**
 * Result recorded for every login attempt.
 * The values mirror {@code ck_audit_login_outcome} on {@code identity_audit.audit_login};
 * adding a value here without the matching changeset breaks the insert.
 */
public enum LoginOutcome {
    SUCCESS,
    INVALID_PASSWORD,
    USER_NOT_FOUND,
    ACCOUNT_LOCKED,
    TOKEN_EXPIRED
}
