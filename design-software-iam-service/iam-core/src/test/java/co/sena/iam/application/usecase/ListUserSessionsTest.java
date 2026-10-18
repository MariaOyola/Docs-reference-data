package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.testsupport.InMemoryRefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-002 E3 — list sessions. */
class ListUserSessionsTest {

    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER = UUID.fromString("c0000000-0000-0000-0000-000000000002");

    private final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
    private ListUserSessions useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUserSessions(refreshTokens);

        refreshTokens.save(RefreshTokenRecord.newSession(
                UUID.randomUUID(), USER_ID, "hash-1", "Chrome/Mac", "1.2.3.4",
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW));

        refreshTokens.save(RefreshTokenRecord.newSession(
                UUID.randomUUID(), USER_ID, "hash-2", "Firefox/Win", "5.6.7.8",
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW.minusSeconds(60)));

        refreshTokens.save(RefreshTokenRecord.newSession(
                UUID.randomUUID(), OTHER_USER, "hash-3", null, null,
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW));
    }

    @Test
    @DisplayName("returns all sessions for the given user regardless of revocation state")
    void listsAllSessionsForUser() {
        List<RefreshTokenRecord> sessions = useCase.listSessions(USER_ID);

        assertEquals(2, sessions.size());
        assertTrue(sessions.stream().allMatch(s -> s.userId().equals(USER_ID)));
    }

    @Test
    @DisplayName("sessions of other users are not included")
    void doesNotLeakOtherUsersSessions() {
        List<RefreshTokenRecord> sessions = useCase.listSessions(USER_ID);

        assertTrue(sessions.stream().noneMatch(s -> s.userId().equals(OTHER_USER)));
    }

    @Test
    @DisplayName("returns empty list when user has no sessions")
    void emptyForUserWithNoSessions() {
        List<RefreshTokenRecord> sessions = useCase.listSessions(UUID.randomUUID());

        assertTrue(sessions.isEmpty());
    }
}
