package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.sena.iam.application.port.out.RateLimiter;
import co.sena.iam.domain.exception.RateLimitExceededException;
import co.sena.iam.domain.model.PasswordResetRecord;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.domain.model.User;
import co.sena.iam.testsupport.InMemoryPasswordResetRepository;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.TestDoubles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-003 E3 — request side. */
class RequestPasswordResetTest {

    private static final Instant NOW = Instant.parse("2026-08-25T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final String EMAIL = "instructor@sena.edu.co";
    private static final String IP = "192.0.2.1";

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryPasswordResetRepository resetRequests = new InMemoryPasswordResetRepository();

    private RateLimiter allowAll;
    private RateLimiter denyAll;
    private RequestPasswordReset useCase;

    @BeforeEach
    void setUp() {
        allowAll = key -> true;
        denyAll  = key -> false;

        users.save(user(USER_ID, EMAIL));

        useCase = new RequestPasswordReset(
                users, resetRequests, TestDoubles.refreshTokenGenerator("tok-abc"), allowAll, clock);
    }

    @Test
    @DisplayName("known email: persists a reset record with 1-hour TTL")
    void knownEmailPersistsRecord() {
        useCase.request(EMAIL, IP);

        assertEquals(1, resetRequests.all().size());
        PasswordResetRecord saved = resetRequests.all().getFirst();
        assertEquals(USER_ID, saved.userId());
        assertEquals(NOW.plus(TokenPolicy.PASSWORD_RESET_TTL), saved.expiresAt());
        assertEquals(IP, saved.ipAddress());
    }

    @Test
    @DisplayName("unknown email: silently returns without saving (no info leak)")
    void unknownEmailDoesNothing() {
        useCase.request("nobody@example.com", IP);

        assertEquals(0, resetRequests.all().size());
    }

    @Test
    @DisplayName("email is normalised to lowercase before lookup")
    void emailNormalisedBeforeLookup() {
        useCase.request("INSTRUCTOR@SENA.EDU.CO", IP);

        assertEquals(1, resetRequests.all().size());
    }

    @Test
    @DisplayName("rate-limit exceeded: throws RateLimitExceededException before touching the repo")
    void rateLimitExceededThrows() {
        RequestPasswordReset limited = new RequestPasswordReset(
                users, resetRequests, TestDoubles.refreshTokenGenerator("tok-abc"), denyAll, clock);

        assertThrows(RateLimitExceededException.class, () -> limited.request(EMAIL, IP));
        assertEquals(0, resetRequests.all().size());
    }

    // -------------------------------------------------------------------------

    private static User user(UUID id, String email) {
        return new User(id, email, TestDoubles.hashOf("Password1!"),
                "Ana", "Instructor", co.sena.iam.domain.model.ActorType.INSTRUCTOR,
                UUID.randomUUID(), true,
                co.sena.iam.domain.model.LockoutState.clear(), false, null);
    }
}
