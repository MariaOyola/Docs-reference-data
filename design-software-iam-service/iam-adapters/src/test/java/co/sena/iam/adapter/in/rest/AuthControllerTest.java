package co.sena.iam.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import co.sena.iam.domain.exception.AccountLockedException;
import co.sena.iam.domain.exception.InvalidCredentialsException;
import co.sena.iam.domain.exception.UnauthenticatedException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract of HU-IAM-001: status codes, envelope and payload shape. */
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID CENTER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String LOGIN_BODY = """
            {"email":"ana@sena.edu.co","password":"Sup3rSecret!"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthenticateUserUseCase authenticateUser;

    @MockBean
    private GetCurrentUserUseCase getCurrentUser;

    @MockBean
    private RefreshAccessTokenUseCase refreshAccessToken;

    @MockBean
    private LogoutUseCase logout;

    @MockBean
    private AccessTokenVerifier tokenVerifier;

    @MockBean
    private RequestPasswordResetUseCase requestPasswordReset;

    @MockBean
    private ConfirmPasswordResetUseCase confirmPasswordReset;

    private static User user() {
        return new User(USER_ID, "ana@sena.edu.co", "irrelevant", "Ana", "Ríos",
                ActorType.INSTRUCTOR, null, true, LockoutState.clear(), false, null);
    }

    private static UserAccess access() {
        return new UserAccess(
                List.of(new RoleAssignment("INSTRUCTOR", CENTER_ID)),
                List.of("schedule.read"),
                List.of("scheduling"));
    }

    @Test
    @DisplayName("E1 POST /auth/login returns 200 with both tokens and the 15-minute lifetime")
    void loginReturnsTokens() throws Exception {
        given(authenticateUser.authenticate(any(LoginCommand.class))).willReturn(
                new AuthenticationResult("access.jwt", "opaque-refresh", 900L, user(), access()));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access.jwt"))
                .andExpect(jsonPath("$.refresh_token").value("opaque-refresh"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.user.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.user.full_name").value("Ana Ríos"))
                .andExpect(jsonPath("$.user.roles[0]").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.user.training_center_id").value(CENTER_ID.toString()))
                .andExpect(jsonPath("$.user.features[0]").value("schedule.read"));
    }

    @Test
    @DisplayName("E2 wrong credentials answer 401 IAM_INVALID_CREDENTIALS in the shared envelope")
    void loginWithWrongCredentialsReturns401() throws Exception {
        willThrow(new InvalidCredentialsException())
                .given(authenticateUser).authenticate(any(LoginCommand.class));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    @DisplayName("E3 a locked account answers 423 IAM_ACCOUNT_LOCKED")
    void loginOnLockedAccountReturns423() throws Exception {
        willThrow(new AccountLockedException(Instant.parse("2026-08-25T12:15:00Z")))
                .given(authenticateUser).authenticate(any(LoginCommand.class));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error.code").value("IAM_ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("a malformed payload answers 400 VALIDATION_ERROR with per-field details")
    void loginWithInvalidPayloadReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isNotEmpty());
    }

    @Test
    @DisplayName("E4 GET /auth/me returns the identity with its roles, features and modules")
    void meReturnsIdentity() throws Exception {
        given(getCurrentUser.currentUser("access.jwt"))
                .willReturn(new AuthenticatedIdentity(user(), access()));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer access.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("ana@sena.edu.co"))
                .andExpect(jsonPath("$.full_name").value("Ana Ríos"))
                .andExpect(jsonPath("$.actor_type").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.roles[0].name").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.roles[0].training_center_id").value(CENTER_ID.toString()))
                .andExpect(jsonPath("$.features[0]").value("schedule.read"))
                .andExpect(jsonPath("$.modules[0]").value("scheduling"));
    }

    @Test
    @DisplayName("E4 GET /auth/me without an Authorization header answers 401")
    void meWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("E4 a token the use case rejects answers 401")
    void meWithRejectedTokenReturns401() throws Exception {
        willThrow(new UnauthenticatedException("expired"))
                .given(getCurrentUser).currentUser("stale.jwt");

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer stale.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }
}
