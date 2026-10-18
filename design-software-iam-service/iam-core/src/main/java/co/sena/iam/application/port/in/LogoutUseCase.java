package co.sena.iam.application.port.in;

import java.util.UUID;

/** HU-IAM-002 E2 — revoke a session so its refresh token can no longer be used. */
public interface LogoutUseCase {

    /**
     * @param sessionId the {@code id} embedded in the bearer access token (claim {@code sid})
     */
    void logout(UUID sessionId);
}
