package co.sena.iam.adapter.in.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.application.port.in.AuthenticateUserUseCase;
import co.sena.iam.application.port.in.ConfirmPasswordResetUseCase;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.LogoutUseCase;
import co.sena.iam.application.port.in.RefreshAccessTokenUseCase;
import co.sena.iam.application.port.in.RequestPasswordResetUseCase;
import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.domain.exception.InvalidResetTokenException;
import co.sena.iam.domain.exception.PasswordChangeRequiredException;
import co.sena.iam.domain.exception.RateLimitExceededException;
import co.sena.iam.domain.exception.TemporaryPasswordExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract for HU-IAM-003: password reset and first-login error codes. */
@WebMvcTest(controllers = AuthController.class)
class PasswordResetControllerTest {

    private static final String RESET_REQUEST_BODY = """
            {"email":"instructor@sena.edu.co"}
            """;
    private static final String RESET_CONFIRM_BODY = """
            {"token":"raw-token-123","new_password":"N3wP@ss!"}
            """;
    private static final String LOGIN_BODY = """
            {"email":"instructor@sena.edu.co","password":"Tmp1@pass"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean private AuthenticateUserUseCase authenticateUser;
    @MockBean private GetCurrentUserUseCase getCurrentUser;
    @MockBean private RefreshAccessTokenUseCase refreshAccessToken;
    @MockBean private LogoutUseCase logout;
    @MockBean private AccessTokenVerifier tokenVerifier;
    @MockBean private RequestPasswordResetUseCase requestPasswordReset;
    @MockBean private ConfirmPasswordResetUseCase confirmPasswordReset;

    // ---------------------------------------------------------------- request

    @Test
    @DisplayName("POST /auth/password-reset/request returns 202 on success")
    void requestReturns202() throws Exception {
        doNothing().when(requestPasswordReset).request(anyString(), anyString());

        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_REQUEST_BODY))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /auth/password-reset/request returns 429 when rate-limited")
    void requestReturns429WhenRateLimited() throws Exception {
        willThrow(new RateLimitExceededException())
                .given(requestPasswordReset).request(anyString(), anyString());

        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_REQUEST_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("IAM_RATE_LIMIT_EXCEEDED"));
    }

    // ---------------------------------------------------------------- confirm

    @Test
    @DisplayName("POST /auth/password-reset/confirm returns 204 on success")
    void confirmReturns204() throws Exception {
        doNothing().when(confirmPasswordReset).confirm(anyString(), anyString());

        mvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_CONFIRM_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /auth/password-reset/confirm returns 400 for invalid token")
    void confirmReturns400ForInvalidToken() throws Exception {
        willThrow(new InvalidResetTokenException())
                .given(confirmPasswordReset).confirm(anyString(), anyString());

        mvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_CONFIRM_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("IAM_INVALID_RESET_TOKEN"));
    }

    // ---------------------------------------------------------------- E1 / E2 via login

    @Test
    @DisplayName("POST /auth/login returns 403 with IAM_PASSWORD_CHANGE_REQUIRED for first-login")
    void loginReturns403WhenMustChangePassword() throws Exception {
        willThrow(new PasswordChangeRequiredException())
                .given(authenticateUser).authenticate(org.mockito.ArgumentMatchers.any());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("IAM_PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    @DisplayName("POST /auth/login returns 401 with IAM_TEMPORARY_PASSWORD_EXPIRED for expired temp password")
    void loginReturns401WhenTempPasswordExpired() throws Exception {
        willThrow(new TemporaryPasswordExpiredException())
                .given(authenticateUser).authenticate(org.mockito.ArgumentMatchers.any());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("IAM_TEMPORARY_PASSWORD_EXPIRED"));
    }
}
