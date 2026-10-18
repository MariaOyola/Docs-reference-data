package co.sena.iam.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.application.port.in.AuthenticatedIdentity;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.GetLoginAuditReportUseCase;
import co.sena.iam.application.port.in.LoginAuditPage;
import co.sena.iam.application.port.in.LoginAuditQuery;
import co.sena.iam.domain.exception.ForbiddenException;
import co.sena.iam.domain.exception.UnauthenticatedException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.LoginAuditRecord;
import co.sena.iam.domain.model.LoginOutcome;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract of HU-IAM-009: status codes, envelope and payload shape. */
@WebMvcTest(controllers = ReportController.class)
class ReportControllerTest {

    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID CENTER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUDIT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetCurrentUserUseCase getCurrentUser;

    @MockBean
    private GetLoginAuditReportUseCase getLoginAuditReport;

    private static User user() {
        return new User(USER_ID, "admin@sena.edu.co", "irrelevant", "Ana", "Ríos",
                ActorType.USER, null, true, LockoutState.clear(), false, null);
    }

    private static UserAccess access() {
        return new UserAccess(
                List.of(new RoleAssignment("SYSTEM_ADMIN", CENTER_ID)),
                List.of("AUDIT_LOG_VIEW"),
                List.of("MOD_AUDIT"));
    }

    // ---------------------------------------------------------------- E1

    @Test
    @DisplayName("E1 GET /reports/login-audit returns a page in the flat envelope")
    void returnsPage() throws Exception {
        given(getCurrentUser.currentUser("access.jwt"))
                .willReturn(new AuthenticatedIdentity(user(), access()));
        LoginAuditRecord entry = new LoginAuditRecord(
                AUDIT_ID, USER_ID, "admin@sena.edu.co", LoginOutcome.SUCCESS,
                "10.0.0.1", "curl/8", Instant.parse("2026-08-27T10:00:00Z"));
        given(getLoginAuditReport.getReport(any(LoginAuditQuery.class)))
                .willReturn(new LoginAuditPage(List.of(entry), "next-cursor", 50, true));

        mvc.perform(get("/api/v1/reports/login-audit").header("Authorization", "Bearer access.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(AUDIT_ID.toString()))
                .andExpect(jsonPath("$.data[0].user_id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data[0].email_attempted").value("admin@sena.edu.co"))
                .andExpect(jsonPath("$.data[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].ip_address").value("10.0.0.1"))
                .andExpect(jsonPath("$.pagination.next_cursor").value("next-cursor"))
                .andExpect(jsonPath("$.pagination.limit").value(50))
                .andExpect(jsonPath("$.pagination.has_more").value(true));
    }

    @Test
    @DisplayName("E1 filters and cursor/limit reach the use case as given")
    void passesFiltersThrough() throws Exception {
        given(getCurrentUser.currentUser("access.jwt"))
                .willReturn(new AuthenticatedIdentity(user(), access()));
        given(getLoginAuditReport.getReport(any(LoginAuditQuery.class)))
                .willReturn(new LoginAuditPage(List.of(), null, 10, false));

        mvc.perform(get("/api/v1/reports/login-audit")
                        .header("Authorization", "Bearer access.jwt")
                        .param("cursor", "abc==")
                        .param("limit", "10")
                        .param("user_id", USER_ID.toString())
                        .param("email", "someone@sena.edu.co")
                        .param("outcome", "INVALID_PASSWORD")
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-27T00:00:00Z"))
                .andExpect(status().isOk());

        ArgumentCaptor<LoginAuditQuery> captor = ArgumentCaptor.forClass(LoginAuditQuery.class);
        verify(getLoginAuditReport).getReport(captor.capture());
        LoginAuditQuery sent = captor.getValue();
        assertThat(sent.callerId()).isEqualTo(USER_ID);
        assertThat(sent.callerTrainingCenterId()).isEqualTo(CENTER_ID);
        assertThat(sent.userId()).isEqualTo(USER_ID);
        assertThat(sent.emailAttempted()).isEqualTo("someone@sena.edu.co");
        assertThat(sent.outcome()).isEqualTo(LoginOutcome.INVALID_PASSWORD);
        assertThat(sent.cursor()).isEqualTo("abc==");
        assertThat(sent.limit()).isEqualTo(10);
    }

    @Test
    @DisplayName("an unknown outcome value answers 400 VALIDATION_ERROR")
    void invalidOutcomeReturns400() throws Exception {
        given(getCurrentUser.currentUser("access.jwt"))
                .willReturn(new AuthenticatedIdentity(user(), access()));

        mvc.perform(get("/api/v1/reports/login-audit")
                        .header("Authorization", "Bearer access.jwt")
                        .param("outcome", "NOT_A_REAL_OUTCOME"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ---------------------------------------------------------------- E2

    @Test
    @DisplayName("E2 no Authorization header answers 401 UNAUTHENTICATED")
    void withoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/reports/login-audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("E2 an invalid token answers 401 UNAUTHENTICATED")
    void invalidTokenReturns401() throws Exception {
        willThrow(new UnauthenticatedException("expired"))
                .given(getCurrentUser).currentUser("stale.jwt");

        mvc.perform(get("/api/v1/reports/login-audit").header("Authorization", "Bearer stale.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("E2 a caller without AUDIT_LOG_VIEW answers 403 FORBIDDEN")
    void withoutGrantReturns403() throws Exception {
        given(getCurrentUser.currentUser("access.jwt"))
                .willReturn(new AuthenticatedIdentity(user(), access()));
        willThrow(new ForbiddenException("no grant"))
                .given(getLoginAuditReport).getReport(any(LoginAuditQuery.class));

        mvc.perform(get("/api/v1/reports/login-audit").header("Authorization", "Bearer access.jwt"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}
