package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.GetUserUseCase;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.User;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-005 E2: fetch a single user by id. */
public final class GetUser implements GetUserUseCase {

    private final UserRepository users;

    public GetUser(UserRepository users) {
        this.users = Objects.requireNonNull(users, "users");
    }

    @Override
    public User getUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
