package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable snapshot of a {@code session.password_reset_request} row. */
public record PasswordResetRecord(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        boolean used,
        Instant requestedAt,
        String ipAddress) {

    public PasswordResetRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }

    public static PasswordResetRecord of(
            UUID userId, String tokenHash, Instant expiresAt, Instant now, String ipAddress) {
        return new PasswordResetRecord(UUID.randomUUID(), userId, tokenHash, expiresAt, false, now, ipAddress);
    }

    public boolean isExpiredAt(Instant now) {
        return now.isAfter(expiresAt);
    }
}
