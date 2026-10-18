package co.sena.iam.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * One row of {@code rbac.user_scope_override}: an exceptional, per-user grant or block of
 * a feature at a given scope.
 *
 * @param allowed   {@code true} grants extra access; {@code false} explicitly blocks it
 * @param expiresAt {@code null} for a permanent override
 */
public record ScopeOverrideRecord(
        UUID id,
        String featureCode,
        ScopeType scopeType,
        boolean allowed,
        String reason,
        UUID grantedBy,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt) {

    public ScopeOverrideRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(featureCode, "featureCode");
        Objects.requireNonNull(scopeType, "scopeType");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(grantedBy, "grantedBy");
    }

    /** Whether this override is still in force at {@code now}. */
    public boolean isActiveAt(java.time.Instant now) {
        return expiresAt == null || expiresAt.toInstant().isAfter(now);
    }
}
