package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.in.ListUsersQuery;
import co.sena.iam.application.port.in.UserPage;
import co.sena.iam.application.port.out.UserQueryPort;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.User;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Paginated read-side adapter for {@code identity.user}. */
@Repository
class UserQueryJdbcAdapter implements UserQueryPort {

    private final JdbcTemplate jdbc;

    UserQueryJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserPage list(ListUsersQuery query) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        boolean hasJoin = query.role() != null || query.trainingCenterId() != null;

        String join = hasJoin
                ? " JOIN rbac.user_role ur ON ur.user_id = u.id"
                  + " JOIN rbac.role r ON r.id = ur.role_id"
                : "";

        if (query.isActive() != null) {
            where.append(" AND u.is_active = ?");
            params.add(query.isActive());
        }
        if (query.role() != null) {
            where.append(" AND r.name = ?");
            params.add(query.role());
        }
        if (query.trainingCenterId() != null) {
            where.append(" AND ur.training_center_id = ?");
            params.add(query.trainingCenterId());
        }
        String condition = where.length() > 0 ? " WHERE " + where.substring(5) : "";

        String countSql = "SELECT COUNT(DISTINCT u.id)"
                + " FROM identity.\"user\" u" + join + condition;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        int offset = (query.page() - 1) * query.size();
        params.add(query.size());
        params.add(offset);

        String dataSql = "SELECT DISTINCT u.id, u.email, u.password_hash, u.first_name, u.last_name,"
                + " u.actor_type, u.actor_id, u.is_active, u.failed_attempts, u.locked_until,"
                + " u.must_change_password, u.password_temporary_expires_at"
                + " FROM identity.\"user\" u" + join + condition
                + " ORDER BY u.id LIMIT ? OFFSET ?";

        List<User> users = jdbc.query(dataSql, (rs, i) -> new User(
                (UUID) rs.getObject("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                ActorType.valueOf(rs.getString("actor_type")),
                (UUID) rs.getObject("actor_id"),
                rs.getBoolean("is_active"),
                new LockoutState(rs.getShort("failed_attempts"), null),
                rs.getBoolean("must_change_password"),
                rs.getObject("password_temporary_expires_at", OffsetDateTime.class) != null
                        ? rs.getObject("password_temporary_expires_at", OffsetDateTime.class).toInstant()
                        : null),
                params.toArray());

        return new UserPage(users, total, query.page(), query.size());
    }
}
