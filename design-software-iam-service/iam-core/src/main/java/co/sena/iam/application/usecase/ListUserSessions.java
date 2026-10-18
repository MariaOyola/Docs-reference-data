package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.ListUserSessionsUseCase;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.domain.model.RefreshTokenRecord;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-002 E3 — return all session rows for a user. */
public final class ListUserSessions implements ListUserSessionsUseCase {

    private final RefreshTokenRepository refreshTokens;

    public ListUserSessions(RefreshTokenRepository refreshTokens) {
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
    }

    @Override
    public List<RefreshTokenRecord> listSessions(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        return refreshTokens.findByUserId(userId);
    }
}
