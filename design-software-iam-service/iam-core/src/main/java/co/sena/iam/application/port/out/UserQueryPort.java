package co.sena.iam.application.port.out;

import co.sena.iam.application.port.in.ListUsersQuery;
import co.sena.iam.application.port.in.UserPage;

/** Outbound port for paginated reads over {@code identity.user}. */
public interface UserQueryPort {

    UserPage list(ListUsersQuery query);
}
