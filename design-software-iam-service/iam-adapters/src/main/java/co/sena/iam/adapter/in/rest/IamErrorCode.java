package co.sena.iam.adapter.in.rest;

/**
 * Stable error codes returned in the shared error envelope.
 *
 * <p>Cross-cutting codes come from {@code shared-contracts/errors/error-codes.yaml};
 * IAM-specific ones are prefixed with the domain, as that file requires.
 */
final class IamErrorCode {

    /** 401 — unknown email, wrong password, or an account that cannot be used. */
    static final String INVALID_CREDENTIALS = "IAM_INVALID_CREDENTIALS";

    /** 423 — the account is serving a lockout period (RN-IAM-01). */
    static final String ACCOUNT_LOCKED = "IAM_ACCOUNT_LOCKED";

    /** 401 — refresh token absent, expired or revoked. */
    static final String TOKEN_REVOKED = "IAM_TOKEN_REVOKED";

    /** 401 — bearer token missing, malformed or expired. */
    static final String UNAUTHENTICATED = "UNAUTHENTICATED";

    /** 403 — authenticated, but the caller lacks the required feature/scope grant (RBAC). */
    static final String FORBIDDEN = "FORBIDDEN";

    /** 404 — session not found for the given user. */
    static final String SESSION_NOT_FOUND = "IAM_SESSION_NOT_FOUND";

    /** 400 — payload rejected by validation. */
    static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    /** 404 — requested resource does not exist. */
    static final String NOT_FOUND = "NOT_FOUND";

    /** 403 — user must change their temporary password before any token is issued. */
    static final String PASSWORD_CHANGE_REQUIRED = "IAM_PASSWORD_CHANGE_REQUIRED";

    /** 401 — temporary password has expired; the user must request a new reset. */
    static final String TEMPORARY_PASSWORD_EXPIRED = "IAM_TEMPORARY_PASSWORD_EXPIRED";

    /** 429 — too many password-reset requests for this email in the current window. */
    static final String RATE_LIMIT_EXCEEDED = "IAM_RATE_LIMIT_EXCEEDED";

    /** 400 — reset token absent, already used, or expired. */
    static final String INVALID_RESET_TOKEN = "IAM_INVALID_RESET_TOKEN";

    /** 404 — user not found by the given id. */
    static final String USER_NOT_FOUND = "IAM_USER_NOT_FOUND";

    /** 409 — email is already registered. */
    static final String EMAIL_CONFLICT = "IAM_EMAIL_CONFLICT";

    /** 404 — role with the given name does not exist. */
    static final String ROLE_NOT_FOUND = "IAM_ROLE_NOT_FOUND";

    /** 409 — role is already assigned to the user (same role + training_center). */
    static final String ROLE_ALREADY_ASSIGNED = "IAM_ROLE_ALREADY_ASSIGNED";

    /** 422 — a scope override referenced a feature code that is not in the catalog. */
    static final String FEATURE_NOT_FOUND = "IAM_FEATURE_NOT_FOUND";

    /** 404 — no scope override with the given id exists for the target user. */
    static final String SCOPE_OVERRIDE_NOT_FOUND = "IAM_SCOPE_OVERRIDE_NOT_FOUND";

    private IamErrorCode() {
    }
}
