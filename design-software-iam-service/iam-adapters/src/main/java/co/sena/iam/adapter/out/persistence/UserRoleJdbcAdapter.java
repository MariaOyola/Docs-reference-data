package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.UserRolePort;
import co.sena.iam.domain.model.UserRoleAssignment;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read/write adapter for {@code rbac.user_role}. */
@Repository
class UserRoleJdbcAdapter implements UserRolePort {

    private final JdbcTemplate jdbc;

    UserRoleJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> findRoleIdByName(String roleName) {
        var rows = jdbc.queryForList(
                "SELECT id FROM rbac.role WHERE name = ?", UUID.class, roleName);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public UserRoleAssignment assign(
            UUID userId, UUID roleId, String roleName,
            UUID trainingCenterId, UUID assignedBy, java.time.OffsetDateTime expiresAt) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO rbac.user_role
                    (id, user_id, role_id, training_center_id, assigned_by, assigned_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id, userId, roleId, trainingCenterId, assignedBy, now,
                expiresAt != null ? Timestamp.from(expiresAt.toInstant()) : null);
        return new UserRoleAssignment(id, userId, roleId, roleName, trainingCenterId,
                assignedBy, now, expiresAt);
    }

    @Override
    public boolean revoke(UUID userId, String roleName, UUID trainingCenterId) {
        int rows;
        if (trainingCenterId == null) {
            rows = jdbc.update("""
                    DELETE FROM rbac.user_role ur
                    USING rbac.role r
                    WHERE ur.role_id = r.id
                      AND ur.user_id = ?
                      AND r.name = ?
                      AND ur.training_center_id IS NULL
                    """, userId, roleName);
        } else {
            rows = jdbc.update("""
                    DELETE FROM rbac.user_role ur
                    USING rbac.role r
                    WHERE ur.role_id = r.id
                      AND ur.user_id = ?
                      AND r.name = ?
                      AND ur.training_center_id = ?
                    """, userId, roleName, trainingCenterId);
        }
        return rows > 0;
    }
}
