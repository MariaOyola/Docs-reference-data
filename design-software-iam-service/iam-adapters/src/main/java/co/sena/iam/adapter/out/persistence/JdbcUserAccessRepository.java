package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.UserAccessRepository;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.UserAccess;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBAC read model: what a user's active role assignments resolve to.
 *
 * <p>Plain SQL rather than JPA — these are projections across four tables, not aggregates
 * this service owns a write model for.
 */
@Repository
class JdbcUserAccessRepository implements UserAccessRepository {

    private static final String ROLES_SQL = """
            SELECT r.name AS role_name, ur.training_center_id
              FROM rbac.user_role ur
              JOIN rbac.role r ON r.id = ur.role_id
             WHERE ur.user_id = ?
               AND (ur.expires_at IS NULL OR ur.expires_at > now())
             ORDER BY r.name
            """;

    private static final String FEATURES_SQL = """
            SELECT DISTINCT f.code
              FROM rbac.user_role ur
              JOIN rbac.role_feature rf ON rf.role_id = ur.role_id
              JOIN rbac_catalog.feature f ON f.id = rf.feature_id
             WHERE ur.user_id = ?
               AND f.is_active
               AND (ur.expires_at IS NULL OR ur.expires_at > now())
             ORDER BY f.code
            """;

    private static final String MODULES_SQL = """
            SELECT DISTINCT m.code
              FROM rbac.user_role ur
              JOIN rbac.role_feature rf ON rf.role_id = ur.role_id
              JOIN rbac_catalog.feature f ON f.id = rf.feature_id
              JOIN rbac_catalog.module m ON m.id = f.module_id
             WHERE ur.user_id = ?
               AND f.is_active
               AND m.is_active
               AND (ur.expires_at IS NULL OR ur.expires_at > now())
             ORDER BY m.code
            """;

    private final JdbcTemplate jdbc;

    JdbcUserAccessRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccess findAccessFor(UUID userId) {
        List<RoleAssignment> roles = jdbc.query(
                ROLES_SQL,
                (rs, rowNum) -> new RoleAssignment(
                        rs.getString("role_name"), rs.getObject("training_center_id", UUID.class)),
                userId);
        List<String> features = jdbc.queryForList(FEATURES_SQL, String.class, userId);
        List<String> modules = jdbc.queryForList(MODULES_SQL, String.class, userId);
        return new UserAccess(roles, features, modules);
    }
}
