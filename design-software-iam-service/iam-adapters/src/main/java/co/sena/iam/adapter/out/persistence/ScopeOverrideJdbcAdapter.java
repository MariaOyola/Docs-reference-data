package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.ScopeOverridePort;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Read/write adapter for {@code rbac.user_scope_override}. */
@Repository
class ScopeOverrideJdbcAdapter implements ScopeOverridePort {

    private static final String ACTIVE_BY_USER_SQL = """
            SELECT so.id, f.code AS feature_code, so.scope_type, so.is_allowed,
                   so.reason, so.granted_by, so.expires_at, so.created_at
              FROM rbac.user_scope_override so
              JOIN rbac_catalog.feature f ON f.id = so.feature_id
             WHERE so.user_id = ?
               AND (so.expires_at IS NULL OR so.expires_at > ?)
             ORDER BY so.created_at DESC
            """;

    private static final RowMapper<ScopeOverrideRecord> ROW_MAPPER = (rs, rowNum) ->
            new ScopeOverrideRecord(
                    rs.getObject("id", UUID.class),
                    rs.getString("feature_code"),
                    ScopeType.fromCode(rs.getString("scope_type")),
                    rs.getBoolean("is_allowed"),
                    rs.getString("reason"),
                    rs.getObject("granted_by", UUID.class),
                    rs.getObject("expires_at", OffsetDateTime.class),
                    rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    ScopeOverrideJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ScopeOverrideRecord> findActiveByUser(UUID userId, Instant now) {
        return jdbc.query(ACTIVE_BY_USER_SQL, ROW_MAPPER, userId, Timestamp.from(now));
    }

    @Override
    public Optional<UUID> findFeatureIdByCode(String featureCode) {
        var ids = jdbc.queryForList(
                "SELECT id FROM rbac_catalog.feature WHERE code = ? AND is_active",
                UUID.class, featureCode);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    @Override
    public ScopeOverrideRecord grant(
            UUID userId, UUID featureId, String featureCode, ScopeType scopeType,
            boolean allowed, String reason, UUID grantedBy, OffsetDateTime expiresAt) {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO rbac.user_scope_override
                    (id, user_id, feature_id, scope_type, is_allowed, reason, granted_by, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, userId, featureId, scopeType.name(), allowed, reason, grantedBy,
                expiresAt != null ? Timestamp.from(expiresAt.toInstant()) : null,
                Timestamp.from(createdAt.toInstant()));
        return new ScopeOverrideRecord(
                id, featureCode, scopeType, allowed, reason, grantedBy, expiresAt, createdAt);
    }

    @Override
    public boolean revoke(UUID userId, UUID overrideId) {
        int rows = jdbc.update(
                "DELETE FROM rbac.user_scope_override WHERE id = ? AND user_id = ?",
                overrideId, userId);
        return rows > 0;
    }
}
