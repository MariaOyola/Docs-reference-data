package co.sena.iam.adapter.out.persistence;

import co.sena.iam.domain.model.PasswordResetRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Row of {@code session.password_reset_request}. Token value is never stored — only the hash. */
@Entity
@Table(name = "password_reset_request", schema = "session")
class PasswordResetRequestEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private boolean used;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    protected PasswordResetRequestEntity() {
    }

    PasswordResetRequestEntity(PasswordResetRecord record) {
        this.id = record.id();
        this.userId = record.userId();
        this.tokenHash = record.tokenHash();
        this.expiresAt = record.expiresAt().atOffset(ZoneOffset.UTC);
        this.used = record.used();
        this.requestedAt = record.requestedAt().atOffset(ZoneOffset.UTC);
        this.ipAddress = record.ipAddress();
    }

    void markUsed() {
        this.used = true;
    }

    PasswordResetRecord toDomain() {
        return new PasswordResetRecord(
                id, userId, tokenHash,
                expiresAt.toInstant(), used,
                requestedAt.toInstant(), ipAddress);
    }

    String getTokenHash() { return tokenHash; }
    boolean isUsed() { return used; }
    OffsetDateTime getExpiresAt() { return expiresAt; }
}
