package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One audited authentication attempt. Mirrors {@code identity_audit.audit_login}.
 *
 * @param userId  null when the email matched no account
 * @param ipAddress nullable; taken from the request
 * @param userAgent nullable; taken from the request
 */
public record LoginAttempt(
        UUID userId,
        String emailAttempted,
        LoginOutcome outcome,
        String ipAddress,
        String userAgent,
        Instant attemptedAt) {

    public LoginAttempt {
        Objects.requireNonNull(emailAttempted, "emailAttempted");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(attemptedAt, "attemptedAt");
    }
}
