package co.sena.iam.adapter.in.rest;

import co.sena.iam.application.port.in.AuthenticatedIdentity;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.GetLoginAuditReportUseCase;
import co.sena.iam.application.port.in.LoginAuditPage;
import co.sena.iam.application.port.in.LoginAuditQuery;
import co.sena.iam.contract.api.ReportsApi;
import co.sena.iam.contract.model.LoginAuditEntry;
import co.sena.iam.contract.model.ReportLoginAudit200Response;
import co.sena.iam.contract.model.ReportLoginAudit200ResponsePagination;
import co.sena.iam.domain.model.LoginAuditRecord;
import co.sena.iam.domain.model.LoginOutcome;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

/** HU-IAM-009 E1-E2 — GET /reports/login-audit. */
@RestController
@RequestMapping("/api/v1")
class ReportController implements ReportsApi {

    private final GetCurrentUserUseCase getCurrentUser;
    private final GetLoginAuditReportUseCase getLoginAuditReport;
    private final HttpServletRequest request;

    ReportController(
            GetCurrentUserUseCase getCurrentUser,
            GetLoginAuditReportUseCase getLoginAuditReport,
            HttpServletRequest request) {
        this.getCurrentUser = getCurrentUser;
        this.getLoginAuditReport = getLoginAuditReport;
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity<ReportLoginAudit200Response> reportLoginAudit(
            String cursor, Integer limit, UUID userId, String email, String outcome,
            OffsetDateTime from, OffsetDateTime to) {
        AuthenticatedIdentity caller = getCurrentUser.currentUser(BearerToken.from(request));

        LoginAuditQuery query = new LoginAuditQuery(
                caller.user().id(),
                caller.access().primaryTrainingCenterId(),
                userId,
                email,
                parseOutcome(outcome),
                from != null ? from.toInstant() : null,
                to != null ? to.toInstant() : null,
                cursor,
                limit != null ? limit : 50);

        LoginAuditPage page = getLoginAuditReport.getReport(query);
        return ResponseEntity.ok(toResponse(page));
    }

    private static LoginOutcome parseOutcome(String outcome) {
        if (outcome == null) {
            return null;
        }
        try {
            return LoginOutcome.valueOf(outcome);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("outcome must be one of " + java.util.Arrays.toString(
                    LoginOutcome.values()), e);
        }
    }

    private static ReportLoginAudit200Response toResponse(LoginAuditPage page) {
        return new ReportLoginAudit200Response()
                .data(page.items().stream().map(ReportController::toEntry).toList())
                .pagination(new ReportLoginAudit200ResponsePagination()
                        .nextCursor(page.nextCursor())
                        .limit(page.limit())
                        .hasMore(page.hasMore()));
    }

    private static LoginAuditEntry toEntry(LoginAuditRecord record) {
        return new LoginAuditEntry()
                .id(record.id())
                .userId(record.userId())
                .emailAttempted(record.emailAttempted())
                .outcome(LoginAuditEntry.OutcomeEnum.fromValue(record.outcome().name()))
                .ipAddress(record.ipAddress())
                .userAgent(record.userAgent())
                .attemptedAt(OffsetDateTime.ofInstant(record.attemptedAt(), ZoneOffset.UTC));
    }
}
