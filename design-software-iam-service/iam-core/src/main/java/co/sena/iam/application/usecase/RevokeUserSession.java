package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.RevokeUserSessionUseCase;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.domain.exception.SessionNotFoundException;
import co.sena.iam.domain.model.RefreshTokenRecord;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-002 E3 — revoke one specific session of a user. */
public final class RevokeUserSession implements RevokeUserSessionUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public RevokeUserSession(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void revokeSession(UUID userId, UUID sessionId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(sessionId, "sessionId");

        RefreshTokenRecord session = refreshTokens.findById(sessionId)
                .filter(s -> s.userId().equals(userId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        refreshTokens.revoke(session.id(), clock.instant());
    }
}
