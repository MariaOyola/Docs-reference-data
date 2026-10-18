package co.sena.iam.api.config;

import co.sena.iam.application.port.in.AssignUserRoleUseCase;
import co.sena.iam.application.port.in.AuthenticateUserUseCase;
import co.sena.iam.application.port.in.ConfirmPasswordResetUseCase;
import co.sena.iam.application.port.in.CreateUserUseCase;
import co.sena.iam.application.port.in.DeactivateUserUseCase;
import co.sena.iam.application.port.in.EvaluateAuthorizationUseCase;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.GetLoginAuditReportUseCase;
import co.sena.iam.application.port.in.GrantScopeOverrideUseCase;
import co.sena.iam.application.port.in.ListScopeOverridesUseCase;
import co.sena.iam.application.port.in.RevokeScopeOverrideUseCase;
import co.sena.iam.application.port.in.GetUserUseCase;
import co.sena.iam.application.port.in.ListModulesUseCase;
import co.sena.iam.application.port.in.ListRoleFeaturesUseCase;
import co.sena.iam.application.port.in.ListRolesUseCase;
import co.sena.iam.application.port.in.ListUserSessionsUseCase;
import co.sena.iam.application.port.in.ListUsersUseCase;
import co.sena.iam.application.port.in.LogoutUseCase;
import co.sena.iam.application.port.in.RefreshAccessTokenUseCase;
import co.sena.iam.application.port.in.RequestPasswordResetUseCase;
import co.sena.iam.application.port.in.RevokeUserRoleUseCase;
import co.sena.iam.application.port.in.RevokeUserSessionUseCase;
import co.sena.iam.application.port.in.UpdateUserUseCase;
import co.sena.iam.application.port.out.AccessTokenIssuer;
import co.sena.iam.adapter.out.client.ActorsActiveFichaClient;
import co.sena.iam.adapter.out.security.JwtAccessTokenIssuer;
import co.sena.iam.adapter.out.security.RsaSigningKeys;
import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.application.port.out.AuthorizationProfilePort;
import co.sena.iam.application.port.out.ScopeOverridePort;
import co.sena.iam.application.port.out.LoginAuditQueryPort;
import co.sena.iam.application.port.out.LoginAuditRepository;
import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.application.port.out.PasswordGenerator;
import co.sena.iam.application.port.out.PasswordHasher;
import co.sena.iam.application.port.out.PasswordResetRepository;
import co.sena.iam.application.port.out.RateLimiter;
import co.sena.iam.application.port.out.RbacCatalogRepository;
import co.sena.iam.application.port.out.RefreshTokenGenerator;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.application.port.out.UserAccessRepository;
import co.sena.iam.application.port.out.UserQueryPort;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.application.port.out.UserRolePort;
import co.sena.iam.application.port.out.UserWritePort;
import co.sena.iam.application.usecase.AssignUserRole;
import co.sena.iam.application.usecase.AuthenticateUser;
import co.sena.iam.application.usecase.ConfirmPasswordReset;
import co.sena.iam.application.usecase.CreateUser;
import co.sena.iam.application.usecase.DeactivateUser;
import co.sena.iam.application.usecase.EvaluateAuthorization;
import co.sena.iam.application.usecase.GetCurrentUser;
import co.sena.iam.application.usecase.GetLoginAuditReport;
import co.sena.iam.application.usecase.GrantScopeOverride;
import co.sena.iam.application.usecase.ListScopeOverrides;
import co.sena.iam.application.usecase.RevokeScopeOverride;
import co.sena.iam.application.usecase.GetUser;
import co.sena.iam.application.usecase.ListModules;
import co.sena.iam.application.usecase.ListRoleFeatures;
import co.sena.iam.application.usecase.ListRoles;
import co.sena.iam.application.usecase.ListUserSessions;
import co.sena.iam.application.usecase.ListUsers;
import co.sena.iam.application.usecase.Logout;
import co.sena.iam.application.usecase.RefreshAccessToken;
import co.sena.iam.application.usecase.RequestPasswordReset;
import co.sena.iam.application.usecase.RevokeUserRole;
import co.sena.iam.application.usecase.RevokeUserSession;
import co.sena.iam.application.usecase.UpdateUser;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring only — use cases live in iam-core and know nothing about Spring. */
@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {

    @Bean
    AccessTokenIssuer accessTokenIssuer(RsaSigningKeys keys, ActorsActiveFichaClient actors) {
        return new JwtAccessTokenIssuer(keys, actors);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository users,
            UserAccessRepository userAccess,
            AuthorizationProfilePort authorizationProfiles,
            LoginAuditRepository auditLog,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokens,
            RefreshTokenGenerator refreshTokenGenerator,
            OutboxPort outbox,
            Clock clock) {
        return new AuthenticateUser(
                users, userAccess, authorizationProfiles, auditLog, refreshTokens,
                passwordHasher, accessTokens, refreshTokenGenerator, outbox, clock);
    }

    @Bean
    GetCurrentUserUseCase getCurrentUserUseCase(
            AccessTokenVerifier accessTokenVerifier,
            UserRepository users,
            UserAccessRepository userAccess) {
        return new GetCurrentUser(accessTokenVerifier, users, userAccess);
    }

    @Bean
    RefreshAccessTokenUseCase refreshAccessTokenUseCase(
            UserRepository users,
            UserAccessRepository userAccess,
            AuthorizationProfilePort authorizationProfiles,
            RefreshTokenRepository refreshTokens,
            AccessTokenIssuer accessTokens,
            RefreshTokenGenerator refreshTokenGenerator,
            Clock clock) {
        return new RefreshAccessToken(
                users, userAccess, authorizationProfiles, refreshTokens,
                accessTokens, refreshTokenGenerator, clock);
    }

    @Bean
    LogoutUseCase logoutUseCase(RefreshTokenRepository refreshTokens, Clock clock) {
        return new Logout(refreshTokens, clock);
    }

    @Bean
    ListUserSessionsUseCase listUserSessionsUseCase(RefreshTokenRepository refreshTokens) {
        return new ListUserSessions(refreshTokens);
    }

    @Bean
    RevokeUserSessionUseCase revokeUserSessionUseCase(RefreshTokenRepository refreshTokens, Clock clock) {
        return new RevokeUserSession(refreshTokens, clock);
    }

    @Bean
    RequestPasswordResetUseCase requestPasswordResetUseCase(
            UserRepository users,
            PasswordResetRepository resetRequests,
            RefreshTokenGenerator tokenGenerator,
            RateLimiter rateLimiter,
            Clock clock) {
        return new RequestPasswordReset(users, resetRequests, tokenGenerator, rateLimiter, clock);
    }

    @Bean
    ConfirmPasswordResetUseCase confirmPasswordResetUseCase(
            PasswordResetRepository resetRequests,
            UserRepository users,
            RefreshTokenRepository sessions,
            PasswordHasher passwordHasher,
            RefreshTokenGenerator tokenGenerator,
            Clock clock) {
        return new ConfirmPasswordReset(resetRequests, users, sessions, passwordHasher, tokenGenerator, clock);
    }

    @Bean
    ListModulesUseCase listModulesUseCase(RbacCatalogRepository catalog) {
        return new ListModules(catalog);
    }

    @Bean
    ListRolesUseCase listRolesUseCase(RbacCatalogRepository catalog) {
        return new ListRoles(catalog);
    }

    @Bean
    ListRoleFeaturesUseCase listRoleFeaturesUseCase(RbacCatalogRepository catalog) {
        return new ListRoleFeatures(catalog);
    }

    // ---------------------------------------------------------------- HU-IAM-005 use cases

    @Bean
    CreateUserUseCase createUserUseCase(
            UserRepository users, UserWritePort userWriter, UserRolePort rolePort,
            PasswordHasher passwordHasher, PasswordGenerator passwordGenerator, OutboxPort outbox) {
        return new CreateUser(users, userWriter, rolePort, passwordHasher, passwordGenerator, outbox);
    }

    @Bean
    GetUserUseCase getUserUseCase(UserRepository users) {
        return new GetUser(users);
    }

    @Bean
    ListUsersUseCase listUsersUseCase(UserQueryPort userQuery) {
        return new ListUsers(userQuery);
    }

    @Bean
    UpdateUserUseCase updateUserUseCase(UserRepository users, UserWritePort userWriter) {
        return new UpdateUser(users, userWriter);
    }

    @Bean
    DeactivateUserUseCase deactivateUserUseCase(
            UserRepository users, UserWritePort userWriter,
            RefreshTokenRepository refreshTokens, OutboxPort outbox) {
        return new DeactivateUser(users, userWriter, refreshTokens, outbox);
    }

    @Bean
    AssignUserRoleUseCase assignUserRoleUseCase(
            UserRepository users, UserRolePort rolePort, OutboxPort outbox) {
        return new AssignUserRole(users, rolePort, outbox);
    }

    @Bean
    RevokeUserRoleUseCase revokeUserRoleUseCase(UserRepository users, UserRolePort rolePort) {
        return new RevokeUserRole(users, rolePort);
    }

    // ---------------------------------------------------------------- HU-IAM-006 use cases

    @Bean
    EvaluateAuthorizationUseCase evaluateAuthorizationUseCase(
            AuthorizationProfilePort authorizationProfiles, Clock clock) {
        return new EvaluateAuthorization(authorizationProfiles, clock);
    }

    @Bean
    GrantScopeOverrideUseCase grantScopeOverrideUseCase(
            UserRepository users, ScopeOverridePort scopeOverrides, OutboxPort outbox) {
        return new GrantScopeOverride(users, scopeOverrides, outbox);
    }

    @Bean
    RevokeScopeOverrideUseCase revokeScopeOverrideUseCase(
            ScopeOverridePort scopeOverrides, OutboxPort outbox) {
        return new RevokeScopeOverride(scopeOverrides, outbox);
    }

    @Bean
    ListScopeOverridesUseCase listScopeOverridesUseCase(
            UserRepository users, ScopeOverridePort scopeOverrides, Clock clock) {
        return new ListScopeOverrides(users, scopeOverrides, clock);
    }

    // ---------------------------------------------------------------- HU-IAM-009 use cases

    @Bean
    GetLoginAuditReportUseCase getLoginAuditReportUseCase(
            EvaluateAuthorizationUseCase evaluateAuthorization, LoginAuditQueryPort loginAuditQuery) {
        return new GetLoginAuditReport(evaluateAuthorization, loginAuditQuery);
    }
}
