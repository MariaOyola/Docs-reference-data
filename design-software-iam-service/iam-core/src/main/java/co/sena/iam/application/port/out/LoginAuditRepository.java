package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.LoginAttempt;

/** Outbound port over {@code identity_audit.audit_login}. Every attempt is recorded. */
public interface LoginAuditRepository {

    void record(LoginAttempt attempt);
}
