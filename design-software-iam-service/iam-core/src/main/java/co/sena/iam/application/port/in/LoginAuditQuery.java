package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.LoginOutcome;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A request for one page of the login-audit report (HU-IAM-009 E1).
 *
 * @param callerId the authenticated caller, evaluated against {@code AUDIT_LOG_VIEW} (E2)
 * @param callerTrainingCenterId the caller's primary training center; {@code null} when their
 *                                roles carry none. The target scope for the authorization check.
 * @param userId optional filter: attempts tied to this account
 * @param emailAttempted optional filter: the email that was attempted
 * @param outcome optional filter
 * @param from optional filter: attempts at or after this instant
 * @param to optional filter: attempts strictly before this instant
 * @param cursor opaque cursor from a previous page; {@code null} for the first page
 * @param limit page size, already clamped by the contract (1..100)
 */
public record LoginAuditQuery(
        UUID callerId,
        UUID callerTrainingCenterId,
        UUID userId,
        String emailAttempted,
        LoginOutcome outcome,
        Instant from,
        Instant to,
        String cursor,
        int limit) {

    public LoginAuditQuery {
        Objects.requireNonNull(callerId, "callerId");
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
