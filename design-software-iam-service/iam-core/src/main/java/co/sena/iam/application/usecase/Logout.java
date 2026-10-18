package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.LogoutUseCase;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-002 E2 — revoke a session by its ID (embedded in the bearer token as {@code sid}). */
public final class Logout implements LogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public Logout(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void logout(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        refreshTokens.revoke(sessionId, clock.instant());
    }
}
