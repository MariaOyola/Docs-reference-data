package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.AuthorizationQuery;
import co.sena.iam.application.port.in.EvaluateAuthorizationUseCase;
import co.sena.iam.application.port.in.GetLoginAuditReportUseCase;
import co.sena.iam.application.port.in.LoginAuditPage;
import co.sena.iam.application.port.in.LoginAuditQuery;
import co.sena.iam.application.port.out.LoginAuditQueryPort;
import co.sena.iam.domain.exception.ForbiddenException;
import co.sena.iam.domain.model.AuthorizationDecision;
import java.util.Objects;

/**
 * HU-IAM-009 E1-E2 — the login-audit report, gated by the {@link #REQUIRED_FEATURE} feature
 * grant that HU-IAM-006's authorization engine resolves.
 */
public final class GetLoginAuditReport implements GetLoginAuditReportUseCase {

    public static final String REQUIRED_FEATURE = "AUDIT_LOG_VIEW";

    private final EvaluateAuthorizationUseCase authorization;
    private final LoginAuditQueryPort query;

    public GetLoginAuditReport(EvaluateAuthorizationUseCase authorization, LoginAuditQueryPort query) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.query = Objects.requireNonNull(query, "query");
    }

    @Override
    public LoginAuditPage getReport(LoginAuditQuery request) {
        Objects.requireNonNull(request, "request");

        AuthorizationDecision decision = authorization.evaluate(new AuthorizationQuery(
                request.callerId(), REQUIRED_FEATURE, request.callerTrainingCenterId()));
        if (!decision.permitted()) {
            throw new ForbiddenException(
                    "Caller lacks " + REQUIRED_FEATURE + " in the required scope: " + decision.reason());
        }

        return query.query(request);
    }
}
