package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.domain.exception.SessionNotFoundException;
import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.testsupport.InMemoryRefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-002 E3 — revoke a specific user session. */
class RevokeUserSessionTest {

    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_SESSION = UUID.fromString("a0000000-0000-0000-0000-000000000002");

    private final java.time.Clock clock = java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC);
    private final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
    private RevokeUserSession useCase;

    @BeforeEach
    void setUp() {
        useCase = new RevokeUserSession(refreshTokens, clock);

        refreshTokens.save(RefreshTokenRecord.newSession(
                SESSION_ID, USER_ID, "hash-1", null, null,
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW));

        refreshTokens.save(RefreshTokenRecord.newSession(
                OTHER_SESSION, OTHER_USER, "hash-2", null, null,
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW));
    }

    @Test
    @DisplayName("revokes a session that belongs to the user")
    void revokesOwnSession() {
        useCase.revokeSession(USER_ID, SESSION_ID);

        RefreshTokenRecord found = refreshTokens.findById(SESSION_ID).orElseThrow();
        assertTrue(found.isRevoked());
        assertEquals(NOW, found.revokedAt());
    }

    @Test
    @DisplayName("throws SessionNotFoundException when session does not exist at all")
    void throwsWhenSessionNotFound() {
        assertThrows(SessionNotFoundException.class,
                () -> useCase.revokeSession(USER_ID, UUID.randomUUID()));
    }

    @Test
    @DisplayName("throws SessionNotFoundException when session belongs to a different user")
    void throwsWhenSessionBelongsToOtherUser() {
        assertThrows(SessionNotFoundException.class,
                () -> useCase.revokeSession(USER_ID, OTHER_SESSION));
    }
}
