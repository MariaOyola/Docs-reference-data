package co.sena.iam.application.port.in;

import java.util.UUID;

/** HU-IAM-002 E3 — revoke one specific session of a user. */
public interface RevokeUserSessionUseCase {

    /**
     * @throws co.sena.iam.domain.exception.SessionNotFoundException session not found for this user
     */
    void revokeSession(UUID userId, UUID sessionId);
}
