package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.LoginAuditRecord;
import java.util.List;
import java.util.Objects;

/** One page of the login-audit report, cursor-paginated. */
public record LoginAuditPage(List<LoginAuditRecord> items, String nextCursor, int limit, boolean hasMore) {

    public LoginAuditPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
}
