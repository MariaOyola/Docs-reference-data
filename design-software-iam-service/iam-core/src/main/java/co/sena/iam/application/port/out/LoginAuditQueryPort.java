package co.sena.iam.application.port.out;

import co.sena.iam.application.port.in.LoginAuditPage;
import co.sena.iam.application.port.in.LoginAuditQuery;

/** Read-only outbound port over {@code identity_audit.audit_login}. */
public interface LoginAuditQueryPort {

    LoginAuditPage query(LoginAuditQuery query);
}
