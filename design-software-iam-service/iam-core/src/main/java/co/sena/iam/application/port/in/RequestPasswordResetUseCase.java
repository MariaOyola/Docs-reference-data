package co.sena.iam.application.port.in;

/** HU-IAM-003 E3: request a password-reset token (blind — always succeeds publicly). */
public interface RequestPasswordResetUseCase {

    /** Generates and persists a reset token for {@code email}. Never reveals existence. */
    void request(String email, String ipAddress);
}
