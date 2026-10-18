package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.RefreshTokenRecord;
import java.util.List;
import java.util.UUID;

/** HU-IAM-002 E3 — list all refresh tokens (active and revoked) for a user. */
public interface ListUserSessionsUseCase {

    List<RefreshTokenRecord> listSessions(UUID userId);
}
