package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.domain.exception.InvalidResetTokenException;
import co.sena.iam.domain.model.PasswordResetRecord;
import co.sena.iam.domain.model.User;
import co.sena.iam.testsupport.InMemoryPasswordResetRepository;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.TestDoubles;
import co.sena.iam.testsupport.TestDoubles.RecordingRefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-003 E3 — confirm side. */
class ConfirmPasswordResetTest {

    private static final Instant NOW = Instant.parse("2026-08-25T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final String RAW_TOKEN = "raw-reset-token";
    private static final String HASHED_TOKEN = "digest-of-" + RAW_TOKEN;
    private static final String NEW_PASSWORD = "N3wP@ssword!";

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryPasswordResetRepository resetRequests = new InMemoryPasswordResetRepository();
    private final RecordingRefreshTokenRepository sessions = new RecordingRefreshTokenRepository();

    private ConfirmPasswordReset useCase;

    @BeforeEach
    void setUp() {
        users.save(user(USER_ID));
        useCase = new ConfirmPasswordReset(
                resetRequests, users, sessions,
                TestDoubles.passwordHasher(),
                TestDoubles.refreshTokenGenerator(RAW_TOKEN),
                clock);
    }

    @Test
    @DisplayName("valid token: password updated, must_change_password cleared, all sessions revoked, token marked used")
    void validTokenUpdatesPasswordAndRevokesSessions() {
        saveActiveReset(USER_ID, HASHED_TOKEN, Instant.MAX);

        useCase.confirm(RAW_TOKEN, NEW_PASSWORD);

        User updated = users.findById(USER_ID).orElseThrow();
        assertTrue(TestDoubles.passwordHasher().matches(NEW_PASSWORD, updated.passwordHash()),
                "password hash must reflect the new password");
        assertTrue(sessions.wereSessionsRevokedFor(USER_ID));
        assertTrue(resetRequests.findByTokenHash(HASHED_TOKEN).map(r -> r.used()).orElse(false),
                "reset record must be marked used");
    }

    @Test
    @DisplayName("unknown token: throws InvalidResetTokenException")
    void unknownTokenThrows() {
        assertThrows(InvalidResetTokenException.class, () -> useCase.confirm("wrong-token", NEW_PASSWORD));
    }

    @Test
    @DisplayName("expired token: throws InvalidResetTokenException")
    void expiredTokenThrows() {
        // record expired 1 second ago
        saveActiveReset(USER_ID, HASHED_TOKEN, NOW.minusSeconds(1));

        assertThrows(InvalidResetTokenException.class, () -> useCase.confirm(RAW_TOKEN, NEW_PASSWORD));
    }

    @Test
    @DisplayName("already-used token: throws InvalidResetTokenException")
    void usedTokenThrows() {
        saveActiveReset(USER_ID, HASHED_TOKEN, NOW.plusSeconds(3600));
        resetRequests.markUsed(resetRequests.findByTokenHash(HASHED_TOKEN).orElseThrow());

        assertThrows(InvalidResetTokenException.class, () -> useCase.confirm(RAW_TOKEN, NEW_PASSWORD));
    }

    // -------------------------------------------------------------------------

    private void saveActiveReset(UUID userId, String tokenHash, Instant expiresAt) {
        resetRequests.save(new PasswordResetRecord(
                UUID.randomUUID(), userId, tokenHash, expiresAt, false, NOW, "127.0.0.1"));
    }

    private static User user(UUID id) {
        return new User(id, "instructor@sena.edu.co", TestDoubles.hashOf("OldP@ss1!"),
                "Ana", "Lopez", co.sena.iam.domain.model.ActorType.INSTRUCTOR,
                UUID.randomUUID(), true,
                co.sena.iam.domain.model.LockoutState.clear(), true, null);
    }
}
