package co.sena.iam.domain.exception;

/** Reset token not found, already used, or expired. Maps to 400. */
public final class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("Password reset token is invalid, already used, or has expired");
    }
}
