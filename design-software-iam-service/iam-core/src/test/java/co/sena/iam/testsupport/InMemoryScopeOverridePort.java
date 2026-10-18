package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.ScopeOverridePort;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory stand-in for {@code rbac.user_scope_override} + the feature-code lookup. */
public final class InMemoryScopeOverridePort implements ScopeOverridePort {

    private final Map<String, UUID> featureIds = new HashMap<>();
    private final List<ScopeOverrideRecord> rows = new ArrayList<>();
    private final Map<UUID, UUID> owners = new HashMap<>();

    public void seedFeature(String code, UUID id) {
        featureIds.put(code, id);
    }

    @Override
    public List<ScopeOverrideRecord> findActiveByUser(UUID userId, Instant now) {
        return rows.stream()
                .filter(r -> ownerOf(r).equals(userId))
                .filter(r -> r.isActiveAt(now))
                .sorted(Comparator.comparing(ScopeOverrideRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<UUID> findFeatureIdByCode(String featureCode) {
        return Optional.ofNullable(featureIds.get(featureCode));
    }

    @Override
    public ScopeOverrideRecord grant(
            UUID userId, UUID featureId, String featureCode, ScopeType scopeType,
            boolean allowed, String reason, UUID grantedBy, OffsetDateTime expiresAt) {
        var record = new ScopeOverrideRecord(
                UUID.randomUUID(), featureCode, scopeType, allowed, reason, grantedBy,
                expiresAt, OffsetDateTime.now(ZoneOffset.UTC));
        rows.add(record);
        owners.put(record.id(), userId);
        return record;
    }

    @Override
    public boolean revoke(UUID userId, UUID overrideId) {
        return rows.removeIf(r -> r.id().equals(overrideId) && ownerOf(r).equals(userId));
    }

    public List<ScopeOverrideRecord> all() {
        return List.copyOf(rows);
    }

    private UUID ownerOf(ScopeOverrideRecord record) {
        return owners.getOrDefault(record.id(), new UUID(0, 0));
    }
}
