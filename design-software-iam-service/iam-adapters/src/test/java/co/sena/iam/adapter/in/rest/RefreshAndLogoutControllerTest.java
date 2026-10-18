package co.sena.iam.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.application.port.in.AuthenticateUserUseCase;
import co.sena.iam.application.port.in.AuthenticationResult;
import co.sena.iam.application.port.in.ConfirmPasswordResetUseCase;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.LogoutUseCase;
import co.sena.iam.application.port.in.RefreshAccessTokenUseCase;
import co.sena.iam.application.port.in.RequestPasswordResetUseCase;
import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.domain.exception.InvalidRefreshTokenException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.TokenClaims;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract for HU-IAM-002 E1 (refresh) and E2 (logout). */
@WebMvcTest(controllers = AuthController.class)
class RefreshAndLogoutControllerTest {

    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID CENTER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

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
        return new User(USER_ID, "ana@sena.edu.co", "hash", "Ana", "Ríos",
                ActorType.INSTRUCTOR, null, true, LockoutState.clear(), false, null);
    }

    private static UserAccess access() {
        return new UserAccess(
                List.of(new RoleAssignment("INSTRUCTOR", CENTER_ID)),
                List.of("schedule.read"),
                List.of("scheduling"));
    }

    // --- E1 refresh ---

    @Test
    @DisplayName("E1 POST /auth/refresh with valid token returns 200 with new access_token")
    void refreshWithValidTokenReturns200() throws Exception {
        given(refreshAccessToken.refresh("valid-refresh")).willReturn(
                new AuthenticationResult("new.access.jwt", "valid-refresh", 900L, user(), access()));

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"valid-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new.access.jwt"))
                .andExpect(jsonPath("$.refresh_token").value("valid-refresh"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900));
    }

    @Test
    @DisplayName("E1 POST /auth/refresh with revoked or expired token returns 401 IAM_TOKEN_REVOKED")
    void refreshWithRevokedTokenReturns401() throws Exception {
        willThrow(new InvalidRefreshTokenException())
                .given(refreshAccessToken).refresh(any());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"bad-refresh\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_TOKEN_REVOKED"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    // --- E2 logout ---

    @Test
    @DisplayName("E2 POST /auth/logout with valid bearer revokes the session and returns 204")
    void logoutWithValidBearerReturns204() throws Exception {
        given(tokenVerifier.verify("access.jwt")).willReturn(new TokenClaims(USER_ID, SESSION_ID));

        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access.jwt"))
                .andExpect(status().isNoContent());

        verify(logout).logout(SESSION_ID);
    }

    @Test
    @DisplayName("E2 POST /auth/logout without an Authorization header returns 401")
    void logoutWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }
}
