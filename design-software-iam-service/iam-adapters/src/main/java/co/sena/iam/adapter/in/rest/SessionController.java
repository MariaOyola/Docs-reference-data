package co.sena.iam.adapter.in.rest;

import co.sena.iam.application.port.in.ListUserSessionsUseCase;
import co.sena.iam.application.port.in.RevokeUserSessionUseCase;
import co.sena.iam.contract.api.SessionsApi;
import co.sena.iam.contract.model.Session;
import co.sena.iam.domain.model.RefreshTokenRecord;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-IAM-002 E3 — {@code GET /users/{id}/sessions} and
 * {@code DELETE /users/{id}/sessions/{session_id}}.
 *
 * <p>Implements the subset of {@link UsersApi} covered by this HU; the rest keep their
 * default {@code 501}.
 */
@RestController
@RequestMapping("/api/v1")
class SessionController implements SessionsApi {

    private final ListUserSessionsUseCase listSessions;
    private final RevokeUserSessionUseCase revokeSession;

    SessionController(ListUserSessionsUseCase listSessions, RevokeUserSessionUseCase revokeSession) {
        this.listSessions = listSessions;
        this.revokeSession = revokeSession;
    }

    @Override
    public ResponseEntity<List<Session>> listUserSessions(UUID id) {
        List<RefreshTokenRecord> sessions = listSessions.listSessions(id);
        return ResponseEntity.ok(sessions.stream().map(SessionController::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> revokeUserSession(UUID id, UUID sessionId) {
        revokeSession.revokeSession(id, sessionId);
        return ResponseEntity.noContent().build();
    }

    private static Session toDto(RefreshTokenRecord r) {
        return new Session(
                r.id(),
                r.createdAt().atOffset(ZoneOffset.UTC),
                r.expiresAt().atOffset(ZoneOffset.UTC),
                r.isRevoked())
                .deviceHint(r.deviceHint())
                .ipAddress(r.ipAddress())
                .revokedAt(r.revokedAt() == null ? null : r.revokedAt().atOffset(ZoneOffset.UTC));
    }
}
