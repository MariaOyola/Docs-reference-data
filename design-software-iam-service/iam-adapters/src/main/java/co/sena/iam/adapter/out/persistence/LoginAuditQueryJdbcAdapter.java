package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.in.LoginAuditPage;
import co.sena.iam.application.port.in.LoginAuditQuery;
import co.sena.iam.application.port.out.LoginAuditQueryPort;
import co.sena.iam.domain.model.LoginAuditRecord;
import co.sena.iam.domain.model.LoginOutcome;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only adapter for the login-audit report (HU-IAM-009 E1) over
 * {@code identity_audit.audit_login}. Cursor-paginated: {@code attempted_at DESC, id DESC}.
 */
@Repository
class LoginAuditQueryJdbcAdapter implements LoginAuditQueryPort {

    private final JdbcTemplate jdbc;

    LoginAuditQueryJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LoginAuditPage query(LoginAuditQuery query) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        if (query.userId() != null) {
            where.append(" AND user_id = ?");
            params.add(query.userId());
        }
        if (query.emailAttempted() != null) {
            where.append(" AND email_attempted = ?");
            params.add(query.emailAttempted());
        }
        if (query.outcome() != null) {
            where.append(" AND outcome = ?");
            params.add(query.outcome().name());
        }
        if (query.from() != null) {
            where.append(" AND attempted_at >= ?");
            params.add(Timestamp.from(query.from()));
        }
        if (query.to() != null) {
            where.append(" AND attempted_at < ?");
            params.add(Timestamp.from(query.to()));
        }

        LoginAuditCursor cursor = query.cursor() != null ? LoginAuditCursor.decode(query.cursor()) : null;
        if (cursor != null) {
            where.append(" AND (attempted_at < ? OR (attempted_at = ? AND id < ?))");
            Timestamp cursorAt = Timestamp.from(cursor.attemptedAt());
            params.add(cursorAt);
            params.add(cursorAt);
            params.add(cursor.id());
        }

        params.add(query.limit() + 1);

        String sql = "SELECT id, user_id, email_attempted, outcome, ip_address, user_agent, attempted_at"
                + " FROM identity_audit.audit_login"
                + " WHERE 1 = 1" + where
                + " ORDER BY attempted_at DESC, id DESC"
                + " LIMIT ?";

        List<LoginAuditRecord> rows = jdbc.query(sql, (rs, i) -> new LoginAuditRecord(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("user_id"),
                rs.getString("email_attempted"),
                LoginOutcome.valueOf(rs.getString("outcome")),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getObject("attempted_at", OffsetDateTime.class).toInstant()),
                params.toArray());

        boolean hasMore = rows.size() > query.limit();
        List<LoginAuditRecord> page = hasMore ? rows.subList(0, query.limit()) : rows;

        String nextCursor = hasMore
                ? LoginAuditCursor.of(
                        page.get(page.size() - 1).attemptedAt(), page.get(page.size() - 1).id()).encode()
                : null;

        return new LoginAuditPage(page, nextCursor, query.limit(), hasMore);
    }
}
