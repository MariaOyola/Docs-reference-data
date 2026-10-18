package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port over {@code rbac.user_scope_override} (+ the {@code rbac_catalog.feature} lookup). */
public interface ScopeOverridePort {

    /** Overrides for the user that are still in force at {@code now}, newest first. */
    List<ScopeOverrideRecord> findActiveByUser(UUID userId, Instant now);

    /** Resolves a feature code to its id; empty when the code is unknown. */
    Optional<UUID> findFeatureIdByCode(String featureCode);

    ScopeOverrideRecord grant(
            UUID userId,
            UUID featureId,
            String featureCode,
            ScopeType scopeType,
            boolean allowed,
            String reason,
            UUID grantedBy,
            OffsetDateTime expiresAt);

    /** Removes the override identified by (userId, overrideId). Returns false when absent. */
    boolean revoke(UUID userId, UUID overrideId);
}
