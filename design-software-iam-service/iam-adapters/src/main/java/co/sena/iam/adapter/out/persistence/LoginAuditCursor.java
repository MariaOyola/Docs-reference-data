package co.sena.iam.adapter.out.persistence;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque cursor for the login-audit report (HU-IAM-009): base64 of {@code "<attemptedAt>|<id>"},
 * the keyset position of the last row on the previous page. Ordering is
 * {@code attempted_at DESC, id DESC}, so the next page selects rows strictly after that position.
 */
final class LoginAuditCursor {

    private final Instant attemptedAt;
    private final UUID id;

    private LoginAuditCursor(Instant attemptedAt, UUID id) {
        this.attemptedAt = attemptedAt;
        this.id = id;
    }

    static LoginAuditCursor of(Instant attemptedAt, UUID id) {
        return new LoginAuditCursor(attemptedAt, id);
    }

    Instant attemptedAt() {
        return attemptedAt;
    }

    UUID id() {
        return id;
    }

    String encode() {
        return Base64.getEncoder().encodeToString((attemptedAt + "|" + id).getBytes());
    }

    /** @throws IllegalArgumentException when the cursor is malformed */
    static LoginAuditCursor decode(String cursor) {
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            int separator = decoded.indexOf('|');
            if (separator < 0) {
                throw new IllegalArgumentException("Malformed cursor");
            }
            Instant attemptedAt = Instant.parse(decoded.substring(0, separator));
            UUID id = UUID.fromString(decoded.substring(separator + 1));
            return new LoginAuditCursor(attemptedAt, id);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed cursor", e);
        }
    }
}
