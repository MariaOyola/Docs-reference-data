package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.ListUsersQuery;
import co.sena.iam.application.port.in.ListUsersUseCase;
import co.sena.iam.application.port.in.UserPage;
import co.sena.iam.application.port.out.UserQueryPort;
import java.util.Objects;

/** HU-IAM-005 E2: paginated listing of users. */
public final class ListUsers implements ListUsersUseCase {

    private final UserQueryPort query;

    public ListUsers(UserQueryPort query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    @Override
    public UserPage listUsers(ListUsersQuery listQuery) {
        return query.list(listQuery);
    }
}
