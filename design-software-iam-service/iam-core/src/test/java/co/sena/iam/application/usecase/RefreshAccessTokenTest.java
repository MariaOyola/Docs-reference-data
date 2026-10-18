package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.sena.iam.application.port.in.AuthenticationResult;
import co.sena.iam.domain.exception.InvalidRefreshTokenException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.AuthorizationProfile;
import co.sena.iam.domain.model.FeatureGrant;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.ScopeType;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import co.sena.iam.testsupport.InMemoryRefreshTokenRepository;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.TestDoubles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-002 E1 — refresh access token. */
class RefreshAccessTokenTest {

    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final String REFRESH_VALUE = "opaque-refresh-token";
    private static final String REFRESH_HASH = "digest-of-" + REFRESH_VALUE;
    private static final String NEW_ACCESS_TOKEN = "new.signed.access.token";

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
    private final UserAccess access = new UserAccess(
            List.of(new RoleAssignment("INSTRUCTOR", UUID.fromString("11111111-1111-1111-1111-111111111111"))),
            List.of("schedule.read"), List.of("scheduling"));
    private final AuthorizationProfile authorizationProfile = new AuthorizationProfile(
            List.of(new FeatureGrant("SCH_VIEW_OWN", ScopeType.OWN_SCHEDULE, null)),
            List.of());
    private final TestDoubles.RecordingAccessTokenIssuer issuer =
            new TestDoubles.RecordingAccessTokenIssuer(NEW_ACCESS_TOKEN);

    private RefreshAccessToken useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshAccessToken(
                users,
                TestDoubles.userAccess(access),
                TestDoubles.authorizationProfile(authorizationProfile),
                refreshTokens,
                issuer,
                TestDoubles.refreshTokenGenerator(REFRESH_VALUE),
                clock);

        users.save(new User(USER_ID, "instructor@sena.edu.co", "hash", "Ana", "Ríos",
                ActorType.INSTRUCTOR, null, true, LockoutState.clear(), false, null));

        refreshTokens.save(RefreshTokenRecord.newSession(
                SESSION_ID, USER_ID, REFRESH_HASH, null, null,
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW));
    }

    @Test
    @DisplayName("valid refresh token returns new access token and echoes the refresh")
    void validRefreshReturnsNewAccessToken() {
        AuthenticationResult result = useCase.refresh(REFRESH_VALUE);

        assertEquals(NEW_ACCESS_TOKEN, result.accessToken());
        assertEquals(REFRESH_VALUE, result.refreshToken());
        assertEquals(TokenPolicy.ACCESS_TOKEN_TTL.toSeconds(), result.expiresInSeconds());
        assertEquals(USER_ID, result.user().id());
    }

    @Test
    @DisplayName("(HU-IAM-010) the recalculated access token carries feature:scope pairs from the HU-006 evaluation")
    void refreshRecalculatesFeatureScopePairs() {
        useCase.refresh(REFRESH_VALUE);

        assertEquals(List.of("SCH_VIEW_OWN:OWN_SCHEDULE"), issuer.lastFeatureScopes());
    }

    @Test
    @DisplayName("unknown hash throws InvalidRefreshTokenException")
    void unknownHashIsRejected() {
        assertThrows(InvalidRefreshTokenException.class, () -> useCase.refresh("unknown-token"));
    }

    @Test
    @DisplayName("expired refresh token throws InvalidRefreshTokenException")
    void expiredTokenIsRejected() {
        String expiredValue = "expired-token-value";
        refreshTokens.save(RefreshTokenRecord.newSession(
                UUID.randomUUID(), USER_ID, "digest-of-" + expiredValue, null, null,
                NOW.minusSeconds(1), NOW.minusSeconds(8 * 24 * 3600)));

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.refresh(expiredValue));
    }

    @Test
    @DisplayName("revoked refresh token throws InvalidRefreshTokenException")
    void revokedTokenIsRejected() {
        String revokedValue = "revoked-token-value";
        refreshTokens.saveRevoked(new RefreshTokenRecord(
                UUID.randomUUID(), USER_ID, "digest-of-" + revokedValue, null, null,
                NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), NOW, true, NOW.minusSeconds(60)));

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.refresh(revokedValue));
    }
}
