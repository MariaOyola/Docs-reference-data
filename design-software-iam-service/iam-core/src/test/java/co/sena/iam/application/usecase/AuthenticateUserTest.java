package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.application.port.in.AuthenticationResult;
import co.sena.iam.application.port.in.LoginCommand;
import co.sena.iam.application.port.out.AuthorizationProfilePort;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.domain.exception.AccountLockedException;
import co.sena.iam.domain.exception.InvalidCredentialsException;
import co.sena.iam.domain.exception.PasswordChangeRequiredException;
import co.sena.iam.domain.exception.TemporaryPasswordExpiredException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.AuthorizationProfile;
import co.sena.iam.domain.model.FeatureGrant;
import co.sena.iam.domain.model.LockoutPolicy;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.LoginOutcome;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.ScopeType;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.RecordingLoginAuditRepository;
import co.sena.iam.testsupport.RecordingOutboxPort;
import co.sena.iam.testsupport.TestDoubles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-001 E1 (login OK), E2 (invalid) and E3 (lockout, RN-IAM-01). */
class AuthenticateUserTest {

    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final String EMAIL = "instructor@sena.edu.co";
    private static final String PASSWORD = "Sup3rSecret!";
    private static final String ACCESS_TOKEN = "signed.access.token";
    private static final String REFRESH_TOKEN = "opaque-refresh-token";

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final RecordingLoginAuditRepository auditLog = new RecordingLoginAuditRepository();
    private final TestDoubles.RecordingRefreshTokenRepository refreshTokens =
            new TestDoubles.RecordingRefreshTokenRepository();
    private final RecordingOutboxPort outbox = new RecordingOutboxPort();
    private final UserAccess access = new UserAccess(
            List.of(new RoleAssignment("INSTRUCTOR", UUID.fromString("11111111-1111-1111-1111-111111111111"))),
            List.of("schedule.read", "schedule.write"),
            List.of("scheduling"));
    private final AuthorizationProfile authorizationProfile = new AuthorizationProfile(
            List.of(new FeatureGrant("SCH_VIEW_OWN", ScopeType.OWN_SCHEDULE, null)),
            List.of());

    private AuthenticateUser authenticateUser;

    @BeforeEach
    void setUp() {
        authenticateUser = newUseCase(refreshTokens);
        users.save(activeUser(LockoutState.clear()));
    }

    private AuthenticateUser newUseCase(RefreshTokenRepository refreshTokenRepository) {
        return newUseCase(refreshTokenRepository, TestDoubles.accessTokenIssuer(ACCESS_TOKEN));
    }

    private AuthenticateUser newUseCase(
            RefreshTokenRepository refreshTokenRepository, co.sena.iam.application.port.out.AccessTokenIssuer issuer) {
        return newUseCase(refreshTokenRepository, issuer, TestDoubles.authorizationProfile(authorizationProfile));
    }

    private AuthenticateUser newUseCase(
            RefreshTokenRepository refreshTokenRepository,
            co.sena.iam.application.port.out.AccessTokenIssuer issuer,
            AuthorizationProfilePort authorizationProfiles) {
        return new AuthenticateUser(
                users,
                TestDoubles.userAccess(access),
                authorizationProfiles,
                auditLog,
                refreshTokenRepository,
                TestDoubles.passwordHasher(),
                issuer,
                TestDoubles.refreshTokenGenerator(REFRESH_TOKEN),
                outbox,
                clock);
    }

    private User activeUser(LockoutState lockout) {
        return new User(USER_ID, EMAIL, TestDoubles.hashOf(PASSWORD), "Ana", "Ríos",
                ActorType.INSTRUCTOR, null, true, lockout, false, null);
    }

    private User tempPasswordUser(boolean expired) {
        Instant expires = expired ? NOW.minusSeconds(1) : NOW.plus(java.time.Duration.ofHours(48));
        return new User(USER_ID, EMAIL, TestDoubles.hashOf(PASSWORD), "Ana", "Ríos",
                ActorType.INSTRUCTOR, null, true, LockoutState.clear(), true, expires);
    }

    // ---------------------------------------------------------------- E1

    @Test
    @DisplayName("E1 valid credentials return both tokens with the contract lifetimes")
    void issuesTokensForValidCredentials() {
        AuthenticationResult result = authenticateUser.authenticate(LoginCommand.of(EMAIL, PASSWORD));

        assertEquals(ACCESS_TOKEN, result.accessToken());
        assertEquals(REFRESH_TOKEN, result.refreshToken());
        assertEquals(TokenPolicy.ACCESS_TOKEN_TTL.toSeconds(), result.expiresInSeconds());
        assertEquals(900L, result.expiresInSeconds());
        assertEquals(List.of("INSTRUCTOR"), result.access().roleNames());
    }

    @Test
    @DisplayName("E1 the session's refresh token is persisted hashed, valid for 7 days")
    void persistsHashedRefreshTokenForSevenDays() {
        authenticateUser.authenticate(
                new LoginCommand(EMAIL, PASSWORD, "10.0.0.7", "JUnit", "laptop"));

        assertEquals(1, refreshTokens.saved().size());
        RefreshTokenRecord saved = refreshTokens.saved().get(0);
        assertEquals(USER_ID, saved.userId());
        assertEquals(NOW.plus(TokenPolicy.REFRESH_TOKEN_TTL), saved.expiresAt());
        assertEquals("10.0.0.7", saved.ipAddress());
        assertEquals("laptop", saved.deviceHint());
        assertTrue(saved.tokenHash().contains("digest-of-"), "only the digest may be stored");
        assertTrue(saved.tokenHash().indexOf(REFRESH_TOKEN) > 0, "digest derives from the token");
    }

    @Test
    @DisplayName("E1 the success is audited and last_login_at is stamped")
    void auditsSuccessAndStampsLastLogin() {
        authenticateUser.authenticate(new LoginCommand(EMAIL, PASSWORD, "10.0.0.7", "JUnit", null));

        assertEquals(List.of(LoginOutcome.SUCCESS), auditLog.outcomes());
        assertEquals(USER_ID, auditLog.last().userId());
        assertEquals(EMAIL, auditLog.last().emailAttempted());
        assertEquals("10.0.0.7", auditLog.last().ipAddress());
        assertEquals(NOW, auditLog.last().attemptedAt());
        assertEquals(NOW, users.lastLoginAt());
    }

    @Test
    @DisplayName("E1 a successful login resets the failed-attempt counter")
    void successResetsTheFailureCounter() {
        users.save(activeUser(new LockoutState(4, null)));

        authenticateUser.authenticate(LoginCommand.of(EMAIL, PASSWORD));

        assertEquals(0, users.lockoutOf(USER_ID).failedAttempts());
        assertNull(users.lockoutOf(USER_ID).lockedUntil());
    }

    @Test
    @DisplayName("E1 the email is matched case-insensitively, as the unique index does")
    void matchesEmailCaseInsensitively() {
        AuthenticationResult result =
                authenticateUser.authenticate(LoginCommand.of("Instructor@SENA.edu.co", PASSWORD));

        assertEquals(ACCESS_TOKEN, result.accessToken());
    }

    @Test
    @DisplayName("E1 (HU-IAM-010) the access token is issued with feature:scope pairs from the HU-006 evaluation")
    void issuesAccessTokenWithFeatureScopePairsFromAuthorizationEvaluation() {
        TestDoubles.RecordingAccessTokenIssuer issuer = new TestDoubles.RecordingAccessTokenIssuer(ACCESS_TOKEN);
        AuthenticateUser useCase = newUseCase(refreshTokens, issuer);

        useCase.authenticate(LoginCommand.of(EMAIL, PASSWORD));

        assertEquals(List.of("SCH_VIEW_OWN:OWN_SCHEDULE"), issuer.lastFeatureScopes());
    }

    // ---------------------------------------------------------------- E4 (HU-IAM-007)

    @Test
    @DisplayName("E4 a successful login stages iam.session.started in the outbox")
    void stagesSessionStartedOnSuccess() {
        authenticateUser.authenticate(LoginCommand.of(EMAIL, PASSWORD));

        OutboxEvent event = outbox.singleEvent();
        assertEquals("iam.session.started", event.eventType());
        assertEquals("iam", event.sourceService());
        assertEquals("1", event.version());
        assertEquals(USER_ID.toString(), event.payload().get("user_id"));
        assertTrue(event.payload().containsKey("session_id"), "the session id is carried in the payload");
    }

    @Test
    @DisplayName("E4 a refused login stages no session event")
    void stagesNothingWhenLoginIsRefused() {
        AuthenticateUser useCase = newUseCase(TestDoubles.refuseToSave());

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(LoginCommand.of(EMAIL, "wrong-password")));

        assertTrue(outbox.isEmpty(), "no session.started for a login that never issued a session");
    }

    // ---------------------------------------------------------------- E2

    @Test
    @DisplayName("E2 a wrong password is refused, audited and counted")
    void refusesWrongPassword() {
        AuthenticateUser useCase = newUseCase(TestDoubles.refuseToSave());

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(LoginCommand.of(EMAIL, "wrong-password")));

        assertEquals(List.of(LoginOutcome.INVALID_PASSWORD), auditLog.outcomes());
        assertEquals(1, users.lockoutOf(USER_ID).failedAttempts());
    }

    @Test
    @DisplayName("E2 an unknown email is refused and audited as USER_NOT_FOUND")
    void refusesUnknownEmail() {
        AuthenticateUser useCase = newUseCase(TestDoubles.refuseToSave());

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(LoginCommand.of("ghost@sena.edu.co", PASSWORD)));

        assertEquals(List.of(LoginOutcome.USER_NOT_FOUND), auditLog.outcomes());
        assertNull(auditLog.last().userId());
    }

    @Test
    @DisplayName("E2 a disabled account is refused like a wrong password, without counting it")
    void refusesDisabledAccount() {
        users.save(new User(USER_ID, EMAIL, TestDoubles.hashOf(PASSWORD), "Ana", "Ríos",
                ActorType.INSTRUCTOR, null, false, LockoutState.clear(), false, null));
        AuthenticateUser useCase = newUseCase(TestDoubles.refuseToSave());

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(LoginCommand.of(EMAIL, PASSWORD)));

        assertEquals(List.of(LoginOutcome.INVALID_PASSWORD), auditLog.outcomes());
        assertEquals(0, users.lockoutOf(USER_ID).failedAttempts());
    }

    // ---------------------------------------------------------------- E3

    @Test
    @DisplayName("E3 five consecutive failures arm the 15-minute lock")
    void fiveFailuresLockForFifteenMinutes() {
        AuthenticateUser useCase = newUseCase(TestDoubles.refuseToSave());

        for (int i = 0; i < LockoutPolicy.SHORT_LOCK_THRESHOLD; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> useCase.authenticate(LoginCommand.of(EMAIL, "wrong-password")));
        }

        LockoutState state = users.lockoutOf(USER_ID);
        assertEquals(5, state.failedAttempts());
        assertEquals(NOW.plus(LockoutPolicy.SHORT_LOCK), state.lockedUntil());
    }

    @Test
    @DisplayName("E3 the tenth failure escalates the lock to 24 hours")
    void tenthFailureLocksForTwentyFourHours() {
        users.save(activeUser(new LockoutState(9, null)));
        AuthenticateUser useCase = newUseCase(TestDoubles.refuseToSave());

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(LoginCommand.of(EMAIL, "wrong-password")));

        LockoutState state = users.lockoutOf(USER_ID);
        assertEquals(10, state.failedAttempts());
        assertEquals(NOW.plus(LockoutPolicy.LONG_LOCK), state.lockedUntil());
    }

    @Test
    @DisplayName("E3 a locked account is rejected before the password is even checked")
    void rejectsLockedAccountEvenWithTheRightPassword() {
        Instant lockedUntil = NOW.plus(LockoutPolicy.SHORT_LOCK);
        users.save(activeUser(new LockoutState(5, lockedUntil)));
        AuthenticateUser useCase = newUseCase(TestDoubles.refuseToSave());

        AccountLockedException thrown = assertThrows(AccountLockedException.class,
                () -> useCase.authenticate(LoginCommand.of(EMAIL, PASSWORD)));

        assertEquals(lockedUntil, thrown.lockedUntil());
        assertEquals(List.of(LoginOutcome.ACCOUNT_LOCKED), auditLog.outcomes());
        assertEquals(5, users.lockoutOf(USER_ID).failedAttempts(), "a blocked attempt does not count");
    }

    @Test
    @DisplayName("E3 once the lock elapses the same credentials work again and clear the counter")
    void allowsLoginOnceTheLockElapsed() {
        users.save(activeUser(new LockoutState(5, NOW.minusSeconds(1))));

        AuthenticationResult result = authenticateUser.authenticate(LoginCommand.of(EMAIL, PASSWORD));

        assertEquals(ACCESS_TOKEN, result.accessToken());
        assertEquals(0, users.lockoutOf(USER_ID).failedAttempts());
    }

    // ---------------------------------------------------------------- HU-IAM-003 E1 / E2

    @Test
    @DisplayName("HU-IAM-003 E1 first login with a temporary (non-expired) password is refused with PasswordChangeRequiredException")
    void firstLoginWithTempPasswordDemandsPwChange() {
        users.save(tempPasswordUser(false));

        assertThrows(PasswordChangeRequiredException.class,
                () -> authenticateUser.authenticate(LoginCommand.of(EMAIL, PASSWORD)));
    }

    @Test
    @DisplayName("HU-IAM-003 E2 first login with an expired temporary password is refused with TemporaryPasswordExpiredException")
    void firstLoginWithExpiredTempPasswordIsRejected() {
        users.save(tempPasswordUser(true));

        assertThrows(TemporaryPasswordExpiredException.class,
                () -> authenticateUser.authenticate(LoginCommand.of(EMAIL, PASSWORD)));
    }
}
