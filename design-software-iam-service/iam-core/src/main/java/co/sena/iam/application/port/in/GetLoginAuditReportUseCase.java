package co.sena.iam.application.port.in;

import co.sena.iam.domain.exception.ForbiddenException;

/** HU-IAM-009 E1 — paginated, read-only report over {@code identity_audit.audit_login}. */
public interface GetLoginAuditReportUseCase {

    /** @throws ForbiddenException when the caller lacks {@code AUDIT_LOG_VIEW} in scope (E2) */
    LoginAuditPage getReport(LoginAuditQuery query);
}
