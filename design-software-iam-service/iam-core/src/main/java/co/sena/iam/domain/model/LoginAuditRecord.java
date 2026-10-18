package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One row read back from {@code identity_audit.audit_login} for the login-audit report
 * (HU-IAM-009). Read-side counterpart of {@link LoginAttempt}, which carries no {@code id}
 * because it is assigned at write time.
 *
 * @param userId null when the attempted email matched no account
 * @param ipAddress nullable; taken from the request
 * @param userAgent nullable; taken from the request
 */
public record LoginAuditRecord(
        UUID id,
        UUID userId,
        String emailAttempted,
        LoginOutcome outcome,
        String ipAddress,
        String userAgent,
        Instant attemptedAt) {

    public LoginAuditRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(emailAttempted, "emailAttempted");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(attemptedAt, "attemptedAt");
    }
}
