package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.UpdateUserCommand;
import co.sena.iam.application.port.in.UpdateUserUseCase;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.application.port.out.UserWritePort;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.User;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-005 E2: update mutable profile fields of a user. */
public final class UpdateUser implements UpdateUserUseCase {

    private final UserRepository users;
    private final UserWritePort userWriter;

    public UpdateUser(UserRepository users, UserWritePort userWriter) {
        this.users = Objects.requireNonNull(users, "users");
        this.userWriter = Objects.requireNonNull(userWriter, "userWriter");
    }

    @Override
    public User updateUser(UUID id, UpdateUserCommand command) {
        users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return userWriter.update(id, command);
    }
}
