package co.sena.iam.application.port.in;

/** E2: returns a paginated list of users matching optional filters. */
public interface ListUsersUseCase {

    UserPage listUsers(ListUsersQuery query);
}
