package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.AuthorizationProfilePort;
import co.sena.iam.domain.model.AuthorizationProfile;
import co.sena.iam.domain.model.FeatureGrant;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the HU-IAM-006 authorization read model: the feature grants a user reaches through
 * their active role assignments, plus their personal scope overrides.
 *
 * <p>Plain SQL over four {@code rbac}/{@code rbac_catalog} tables — a projection, not an
 * aggregate this service owns a write model for (same rationale as {@code JdbcUserAccessRepository}).
 */
@Repository
class JdbcAuthorizationProfileRepository implements AuthorizationProfilePort {

    private static final String GRANTS_SQL = """
            SELECT f.code AS feature_code, rf.scope_type, ur.training_center_id
              FROM rbac.user_role ur
              JOIN rbac.role_feature rf ON rf.role_id = ur.role_id
              JOIN rbac_catalog.feature f ON f.id = rf.feature_id
             WHERE ur.user_id = ?
               AND f.is_active
               AND (ur.expires_at IS NULL OR ur.expires_at > ?)
            """;

    private static final String OVERRIDES_SQL = """
            SELECT so.id, f.code AS feature_code, so.scope_type, so.is_allowed,
                   so.reason, so.granted_by, so.expires_at, so.created_at
              FROM rbac.user_scope_override so
              JOIN rbac_catalog.feature f ON f.id = so.feature_id
             WHERE so.user_id = ?
            """;

    private static final RowMapper<FeatureGrant> GRANT_MAPPER = (rs, rowNum) ->
            new FeatureGrant(
                    rs.getString("feature_code"),
                    ScopeType.fromCode(rs.getString("scope_type")),
                    rs.getObject("training_center_id", UUID.class));

    private static final RowMapper<ScopeOverrideRecord> OVERRIDE_MAPPER = (rs, rowNum) ->
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

    JdbcAuthorizationProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorizationProfile loadProfile(UUID userId) {
        Timestamp now = Timestamp.from(Instant.now());
        List<FeatureGrant> grants = jdbc.query(GRANTS_SQL, GRANT_MAPPER, userId, now);
        List<ScopeOverrideRecord> overrides = jdbc.query(OVERRIDES_SQL, OVERRIDE_MAPPER, userId);
        return new AuthorizationProfile(grants, overrides);
    }
}
