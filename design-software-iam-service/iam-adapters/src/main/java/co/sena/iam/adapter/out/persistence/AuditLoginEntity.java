package co.sena.iam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Row of {@code identity_audit.audit_login}. Append-only. */
@Entity
@Table(name = "audit_login", schema = "identity_audit")
class AuditLoginEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_attempted", nullable = false, length = 255)
    private String emailAttempted;

    /** Constrained by {@code ck_audit_login_outcome}. */
    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt;

    protected AuditLoginEntity() {
    }

    AuditLoginEntity(
            UUID id,
            UUID userId,
            String emailAttempted,
            String outcome,
            String ipAddress,
            String userAgent,
            OffsetDateTime attemptedAt) {
        this.id = id;
        this.userId = userId;
        this.emailAttempted = emailAttempted;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.attemptedAt = attemptedAt;
    }
}
