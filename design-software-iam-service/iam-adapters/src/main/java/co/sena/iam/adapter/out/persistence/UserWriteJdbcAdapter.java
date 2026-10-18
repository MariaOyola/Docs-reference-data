package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.in.UpdateUserCommand;
import co.sena.iam.application.port.out.UserWritePort;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.User;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Write-side adapter for {@code identity.user} — save, update, and soft-delete. */
@Repository
class UserWriteJdbcAdapter implements UserWritePort {

    private final JdbcTemplate jdbc;

    UserWriteJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(User user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name,
                     actor_type, actor_id, is_active, must_change_password,
                     failed_attempts, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                user.id(), user.email(), user.passwordHash(),
                user.firstName(), user.lastName(),
                user.actorType().name(), user.actorId(),
                user.active(), user.mustChangePassword(), now, now);
    }

    @Override
    public User update(UUID userId, UpdateUserCommand cmd) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                UPDATE identity."user"
                   SET first_name = COALESCE(?, first_name),
                       last_name  = COALESCE(?, last_name),
                       actor_type = COALESCE(?, actor_type),
                       actor_id   = CASE WHEN ? THEN NULL ELSE COALESCE(?, actor_id) END,
                       is_active  = COALESCE(?, is_active),
                       updated_at = ?
                 WHERE id = ?
                """,
                cmd.firstName(), cmd.lastName(),
                cmd.actorType() != null ? cmd.actorType().name() : null,
                cmd.actorId() == null && cmd.actorType() != null, cmd.actorId(),
                cmd.isActive(), now, userId);
        return jdbc.queryForObject("""
                SELECT id, email, password_hash, first_name, last_name,
                       actor_type, actor_id, is_active, failed_attempts, locked_until,
                       must_change_password, password_temporary_expires_at
                  FROM identity."user" WHERE id = ?
                """, (rs, i) -> new User(
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
                userId);
    }

    @Override
    public void deactivate(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                UPDATE identity."user"
                   SET is_active = false, updated_at = ?
                 WHERE id = ?
                """, now, userId);
    }
}
