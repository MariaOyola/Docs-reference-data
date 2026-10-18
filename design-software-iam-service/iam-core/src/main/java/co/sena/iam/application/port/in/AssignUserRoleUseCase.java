package co.sena.iam.application.port.in;

/** E4: assigns a role to a user, optionally scoped to a training center. */
public interface AssignUserRoleUseCase {

    UserRoleResult assign(AssignRoleCommand command);
}
