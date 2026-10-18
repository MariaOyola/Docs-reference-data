package co.sena.iam.domain.exception;

/**
 * HU-IAM-003 E1: user authenticated with a temporary password and must set a new one
 * before any token is issued.
 */
public final class PasswordChangeRequiredException extends RuntimeException {

    public PasswordChangeRequiredException() {
        super("Password change required before a session can be issued");
    }
}
