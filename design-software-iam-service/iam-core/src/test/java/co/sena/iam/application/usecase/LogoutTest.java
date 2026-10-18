package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.testsupport.InMemoryRefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-002 E2 — logout revokes the refresh token. */
class LogoutTest {

    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();

    private Logout useCase;

    @BeforeEach
    void setUp() {
        useCase = new Logout(refreshTokens, clock);

        refreshTokens.save(RefreshTokenRecord.newSession(
                SESSION_ID, USER_ID, "any-hash", null, null,
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW));
    }

    @Test
    @DisplayName("logout marks the session as revoked")
    void logoutRevokesTheSession() {
        useCase.logout(SESSION_ID);

        RefreshTokenRecord found = refreshTokens.findById(SESSION_ID).orElseThrow();
        assertTrue(found.isRevoked());
        assertFalse(found.revokedAt() == null);
    }

    @Test
    @DisplayName("logout is idempotent: already-revoked session silently succeeds")
    void logoutIsIdempotent() {
        useCase.logout(SESSION_ID);
        useCase.logout(SESSION_ID); // must not throw
    }

    @Test
    @DisplayName("logout on a non-existent session silently succeeds")
    void logoutUnknownSessionIsNoop() {
        useCase.logout(UUID.randomUUID()); // must not throw
    }
}
