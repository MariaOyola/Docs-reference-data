package co.sena.iam.application.port.in;

import java.util.UUID;

/** E3: soft-deletes a user account and revokes all active sessions. */
public interface DeactivateUserUseCase {

    void deactivate(UUID userId);
}
