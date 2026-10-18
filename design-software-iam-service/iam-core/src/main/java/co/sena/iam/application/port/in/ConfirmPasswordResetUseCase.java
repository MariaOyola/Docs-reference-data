package co.sena.iam.application.port.in;

/** HU-IAM-003 E3: confirm a password-reset using the token received. */
public interface ConfirmPasswordResetUseCase {

    /**
     * Validates {@code token}, sets {@code newPassword} as the user's password, clears the
     * must-change flag, and revokes all active sessions.
     *
     * @throws co.sena.iam.domain.exception.InvalidResetTokenException if invalid/used/expired
     */
    void confirm(String token, String newPassword);
}
