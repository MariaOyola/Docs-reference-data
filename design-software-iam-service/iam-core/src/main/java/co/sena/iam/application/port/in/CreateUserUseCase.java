package co.sena.iam.application.port.in;

/** E1: creates a user account and returns the generated temporary password. */
public interface CreateUserUseCase {

    UserCreateResult create(CreateUserCommand command);
}
