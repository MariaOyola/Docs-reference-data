package co.sena.iam.domain.exception;

/**
 * HU-IAM-003 E2: the temporary password window (72 h) has elapsed.
 * The user must request a new reset token.
 */
public final class TemporaryPasswordExpiredException extends RuntimeException {

    public TemporaryPasswordExpiredException() {
        super("Temporary password has expired");
    }
}
