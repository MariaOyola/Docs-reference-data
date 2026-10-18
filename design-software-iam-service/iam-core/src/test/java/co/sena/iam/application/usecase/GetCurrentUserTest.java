package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.sena.iam.application.port.in.AuthenticatedIdentity;
import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.domain.exception.UnauthenticatedException;
import co.sena.iam.domain.model.TokenClaims;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.TestDoubles;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-001 E4 — GET /auth/me. */
class GetCurrentUserTest {

    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000009");
    private static final String TOKEN = "signed.access.token";

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final UserAccess access = new UserAccess(
            List.of(new RoleAssignment("COORDINATOR", null)),
            List.of("schedule.read"),
            List.of("scheduling"));
    private static final UUID SESSION_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    private final AccessTokenVerifier validToken = token -> {
        if (!TOKEN.equals(token)) {
            throw new UnauthenticatedException("bad token");
        }
        return new TokenClaims(USER_ID, SESSION_ID);
    };

    private GetCurrentUser getCurrentUser;

    @BeforeEach
    void setUp() {
        getCurrentUser = new GetCurrentUser(validToken, users, TestDoubles.userAccess(access));
        users.save(user(true));
    }

    private User user(boolean active) {
        return new User(USER_ID, "coord@sena.edu.co", "irrelevant", "Luz", "Peña",
                ActorType.USER, null, active, LockoutState.clear(), false, null);
    }

    @Test
    @DisplayName("a valid token resolves the identity with its roles and features")
    void resolvesIdentityForValidToken() {
        AuthenticatedIdentity identity = getCurrentUser.currentUser(TOKEN);

        assertEquals(USER_ID, identity.user().id());
        assertEquals("Luz Peña", identity.user().fullName());
        assertEquals(List.of("COORDINATOR"), identity.access().roleNames());
        assertEquals(List.of("schedule.read"), identity.access().featureCodes());
        assertEquals(List.of("scheduling"), identity.access().moduleCodes());
    }

    @Test
    @DisplayName("a token the verifier rejects is unauthenticated")
    void rejectsInvalidToken() {
        assertThrows(UnauthenticatedException.class, () -> getCurrentUser.currentUser("forged"));
    }

    @Test
    @DisplayName("a token whose subject no longer exists is unauthenticated")
    void rejectsTokenWithUnknownSubject() {
        GetCurrentUser useCase = new GetCurrentUser(
                validToken, new InMemoryUserRepository(), TestDoubles.userAccess(access));

        assertThrows(UnauthenticatedException.class, () -> useCase.currentUser(TOKEN));
    }

    @Test
    @DisplayName("a disabled account cannot use a still-valid token")
    void rejectsDisabledAccount() {
        users.save(user(false));

        assertThrows(UnauthenticatedException.class, () -> getCurrentUser.currentUser(TOKEN));
    }
}
