package co.sena.iam.domain.exception;

/** The refresh token is absent, expired, or revoked → 401 IAM_TOKEN_REVOKED. */
public final class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token invalid, expired, or revoked");
    }
}
