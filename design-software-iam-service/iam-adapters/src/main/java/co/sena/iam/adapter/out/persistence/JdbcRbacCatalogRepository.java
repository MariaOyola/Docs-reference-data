package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.RbacCatalogRepository;
import co.sena.iam.domain.model.RbacFeature;
import co.sena.iam.domain.model.RbacModule;
import co.sena.iam.domain.model.RbacRole;
import co.sena.iam.domain.model.RoleFeatureEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-IAM-004 — read-only catalog queries over rbac_catalog.module/feature and rbac.role/role_feature.
 *
 * <p>Uses plain JDBC (same pattern as JdbcUserAccessRepository) because these are cross-schema
 * projections, not aggregates owned by this service's write model.
 *
 * <p>scope_type in the module/features view is resolved as the most permissive scope assigned to
 * each feature across all roles (COALESCE with 'GLOBAL' fallback for unassigned features).
 * This is a catalog view; the authoritative per-role scope lives in the roles/{id}/features endpoint.
 */
@Repository
class JdbcRbacCatalogRepository implements RbacCatalogRepository {

    /**
     * Fetches all active modules with their active features.
     * scope_type is the broadest scope this feature has across all roles
     * (correlated subquery ordered to pick 'GLOBAL' > other scopes alphabetically via CASE rank).
     */
    private static final String MODULES_SQL = """
            SELECT m.id        AS module_id,
                   m.code      AS module_code,
                   m.name      AS module_name,
                   m.description AS module_desc,
                   m.display_order,
                   m.icon_key,
                   f.code      AS feature_code,
                   f.name      AS feature_name,
                   f.description AS feature_desc,
                   f.action_level,
                   COALESCE(
                     (SELECT rf.scope_type
                        FROM rbac.role_feature rf
                       WHERE rf.feature_id = f.id
                       ORDER BY CASE rf.scope_type
                                  WHEN 'GLOBAL'              THEN 1
                                  WHEN 'TRAINING_CENTER'     THEN 2
                                  WHEN 'AREA'                THEN 3
                                  WHEN 'OWN_FICHAS'          THEN 4
                                  WHEN 'OWN_SCHEDULE'        THEN 5
                                  WHEN 'OWN_PROFILE'         THEN 6
                                  WHEN 'OWN_FICHA_AS_LEARNER' THEN 7
                                  ELSE 8
                                END
                       LIMIT 1),
                     'GLOBAL'
                   ) AS scope_type
              FROM rbac_catalog.module m
              JOIN rbac_catalog.feature f ON f.module_id = m.id
             WHERE m.is_active AND f.is_active
             ORDER BY m.display_order, f.code
            """;

    private static final String ROLES_SQL = """
            SELECT id, name, display_name, description, is_system_role, created_at
              FROM rbac.role
             ORDER BY name
            """;

    private static final String ROLE_EXISTS_SQL = """
            SELECT COUNT(*) FROM rbac.role WHERE id = ?
            """;

    private static final String ROLE_FEATURES_SQL = """
            SELECT f.code AS feature_code,
                   f.name AS feature_name,
                   f.action_level,
                   rf.scope_type
              FROM rbac.role_feature rf
              JOIN rbac_catalog.feature f ON f.id = rf.feature_id
             WHERE rf.role_id = ?
             ORDER BY f.code
            """;

    private final JdbcTemplate jdbc;

    JdbcRbacCatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RbacModule> listModules() {
        Map<UUID, RbacModule> byId = new LinkedHashMap<>();
        Map<UUID, List<RbacFeature>> featuresByModule = new LinkedHashMap<>();

        jdbc.query(MODULES_SQL, rs -> {
            UUID moduleId = rs.getObject("module_id", UUID.class);
            byId.computeIfAbsent(moduleId, id -> moduleFrom(rs, id));
            featuresByModule.computeIfAbsent(moduleId, id -> new ArrayList<>())
                    .add(featureFrom(rs));
        });

        return byId.entrySet().stream()
                .map(e -> {
                    RbacModule m = e.getValue();
                    return new RbacModule(m.id(), m.code(), m.name(), m.description(),
                            m.displayOrder(), m.iconKey(), featuresByModule.getOrDefault(m.id(), List.of()));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RbacRole> listRoles() {
        return jdbc.query(ROLES_SQL, (rs, rowNum) -> roleFrom(rs));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<RoleFeatureEntry>> findRoleFeatures(UUID roleId) {
        Integer count = jdbc.queryForObject(ROLE_EXISTS_SQL, Integer.class, roleId);
        if (count == null || count == 0) {
            return Optional.empty();
        }
        List<RoleFeatureEntry> entries = jdbc.query(
                ROLE_FEATURES_SQL,
                (rs, rowNum) -> new RoleFeatureEntry(
                        rs.getString("feature_code"),
                        rs.getString("feature_name"),
                        rs.getString("action_level"),
                        rs.getString("scope_type")),
                roleId);
        return Optional.of(entries);
    }

    private static RbacModule moduleFrom(ResultSet rs, UUID moduleId) {
        try {
            return new RbacModule(
                    moduleId,
                    rs.getString("module_code"),
                    rs.getString("module_name"),
                    rs.getString("module_desc"),
                    rs.getInt("display_order"),
                    rs.getString("icon_key"),
                    List.of());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static RbacFeature featureFrom(ResultSet rs) {
        try {
            return new RbacFeature(
                    rs.getString("feature_code"),
                    rs.getString("feature_name"),
                    rs.getString("feature_desc"),
                    rs.getString("action_level"),
                    rs.getString("scope_type"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static RbacRole roleFrom(ResultSet rs) throws SQLException {
        return new RbacRole(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("display_name"),
                rs.getString("description"),
                rs.getBoolean("is_system_role"),
                rs.getTimestamp("created_at").toInstant());
    }
}
