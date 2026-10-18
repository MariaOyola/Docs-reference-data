package co.sena.iam.adapter.in.rest;

import co.sena.iam.application.port.in.AuthenticateUserUseCase;
import co.sena.iam.application.port.in.AuthenticatedIdentity;
import co.sena.iam.application.port.in.AuthenticationResult;
import co.sena.iam.application.port.in.ConfirmPasswordResetUseCase;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.LoginCommand;
import co.sena.iam.application.port.in.LogoutUseCase;
import co.sena.iam.application.port.in.RefreshAccessTokenUseCase;
import co.sena.iam.application.port.in.RequestPasswordResetUseCase;
import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.contract.api.AuthApi;
import co.sena.iam.contract.model.AuthUser;
import co.sena.iam.contract.model.LoginRequest;
import co.sena.iam.contract.model.MeResponse;
import co.sena.iam.contract.model.MeResponseRolesInner;
import co.sena.iam.contract.model.PasswordResetConfirm;
import co.sena.iam.contract.model.PasswordResetRequest;
import co.sena.iam.contract.model.RefreshRequest;
import co.sena.iam.contract.model.TokenResponse;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.TokenClaims;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-IAM-001 E1-E4 and HU-IAM-002 E1-E2.
 * {@code POST /auth/login}, {@code GET /auth/me}, {@code POST /auth/refresh},
 * {@code POST /auth/logout}.
 */
@RestController
@RequestMapping("/api/v1")
class AuthController implements AuthApi {

    private static final int IP_COLUMN_LENGTH = 45;
    private static final int DEVICE_HINT_COLUMN_LENGTH = 200;

    private final AuthenticateUserUseCase authenticateUser;
    private final GetCurrentUserUseCase getCurrentUser;
    private final RefreshAccessTokenUseCase refreshAccessToken;
    private final LogoutUseCase logout;
    private final RequestPasswordResetUseCase requestPasswordReset;
    private final ConfirmPasswordResetUseCase confirmPasswordReset;
    private final AccessTokenVerifier tokenVerifier;
    private final HttpServletRequest request;

    AuthController(
            AuthenticateUserUseCase authenticateUser,
            GetCurrentUserUseCase getCurrentUser,
            RefreshAccessTokenUseCase refreshAccessToken,
            LogoutUseCase logout,
            RequestPasswordResetUseCase requestPasswordReset,
            ConfirmPasswordResetUseCase confirmPasswordReset,
            AccessTokenVerifier tokenVerifier,
            HttpServletRequest request) {
        this.authenticateUser = authenticateUser;
        this.getCurrentUser = getCurrentUser;
        this.refreshAccessToken = refreshAccessToken;
        this.logout = logout;
        this.requestPasswordReset = requestPasswordReset;
        this.confirmPasswordReset = confirmPasswordReset;
        this.tokenVerifier = tokenVerifier;
        this.request = request;
    }

    @Override
    public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
        AuthenticationResult result = authenticateUser.authenticate(new LoginCommand(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                clientIp(),
                header("User-Agent"),
                truncate(header("X-Device-Hint"), DEVICE_HINT_COLUMN_LENGTH)));

        return ResponseEntity.ok(toTokenResponse(result));
    }

    @Override
    public ResponseEntity<TokenResponse> refreshToken(RefreshRequest refreshRequest) {
        AuthenticationResult result = refreshAccessToken.refresh(refreshRequest.getRefreshToken());
        return ResponseEntity.ok(toTokenResponse(result));
    }

    @Override
    public ResponseEntity<Void> logout() {
        TokenClaims claims = tokenVerifier.verify(BearerToken.from(request));
        this.logout.logout(claims.sessionId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> requestPasswordReset(PasswordResetRequest body) {
        requestPasswordReset.request(body.getEmail(), clientIp());
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> confirmPasswordReset(PasswordResetConfirm body) {
        confirmPasswordReset.confirm(body.getToken(), body.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MeResponse> getMe() {
        AuthenticatedIdentity identity = getCurrentUser.currentUser(BearerToken.from(request));
        User user = identity.user();
        UserAccess access = identity.access();

        return ResponseEntity.ok(new MeResponse()
                .id(user.id())
                .email(user.email())
                .fullName(user.fullName())
                .actorType(MeResponse.ActorTypeEnum.fromValue(user.actorType().name()))
                .actorId(user.actorId())
                .roles(roles(access))
                .features(access.featureCodes())
                .modules(access.moduleCodes()));
    }

    private static TokenResponse toTokenResponse(AuthenticationResult result) {
        return new TokenResponse()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .tokenType(TokenResponse.TokenTypeEnum.BEARER)
                .expiresIn(Math.toIntExact(result.expiresInSeconds()))
                .user(authUser(result.user(), result.access()));
    }

    private static AuthUser authUser(User user, UserAccess access) {
        return new AuthUser()
                .id(user.id())
                .email(user.email())
                .fullName(user.fullName())
                .roles(access.roleNames())
                .trainingCenterId(access.primaryTrainingCenterId())
                .features(access.featureCodes());
    }

    private static List<MeResponseRolesInner> roles(UserAccess access) {
        return access.roles().stream()
                .map(AuthController::role)
                .toList();
    }

    private static MeResponseRolesInner role(RoleAssignment assignment) {
        return new MeResponseRolesInner()
                .name(assignment.roleName())
                .trainingCenterId(assignment.trainingCenterId());
    }

    private String clientIp() {
        String forwarded = header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), IP_COLUMN_LENGTH);
        }
        return truncate(request.getRemoteAddr(), IP_COLUMN_LENGTH);
    }

    private String header(String name) {
        return request.getHeader(name);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
