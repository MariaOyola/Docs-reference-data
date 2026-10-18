package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.ScopeType;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to grant or block a feature for one user, exceptionally.
 *
 * @param allowed   {@code true} grants extra access; {@code false} explicitly blocks it
 * @param grantedBy the authenticated administrator performing the override
 * @param expiresAt {@code null} for a permanent override
 */
public record GrantScopeOverrideCommand(
        UUID userId,
        String featureCode,
        ScopeType scopeType,
        boolean allowed,
        String reason,
        UUID grantedBy,
        OffsetDateTime expiresAt) {

    public GrantScopeOverrideCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(featureCode, "featureCode");
        Objects.requireNonNull(scopeType, "scopeType");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(grantedBy, "grantedBy");
    }
}
