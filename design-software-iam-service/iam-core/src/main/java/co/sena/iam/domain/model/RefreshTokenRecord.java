package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted side of a session. Mirrors {@code session.refresh_token}.
 * Only the digest is stored — never the value handed to the client.
 *
 * @param deviceHint nullable
 * @param ipAddress  nullable
 * @param revokedAt  nullable
 */
public record RefreshTokenRecord(
        UUID id,
        UUID userId,
        String tokenHash,
        String deviceHint,
        String ipAddress,
        Instant expiresAt,
        Instant createdAt,
        boolean isRevoked,
        Instant revokedAt) {

    public RefreshTokenRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Convenience factory for a new, non-revoked session row. */
    public static RefreshTokenRecord newSession(
            UUID id, UUID userId, String tokenHash,
            String deviceHint, String ipAddress, Instant expiresAt, Instant createdAt) {
        return new RefreshTokenRecord(id, userId, tokenHash, deviceHint, ipAddress, expiresAt, createdAt, false, null);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
