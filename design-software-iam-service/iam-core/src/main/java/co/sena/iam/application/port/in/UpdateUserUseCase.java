package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.User;
import java.util.UUID;

/** E2: updates mutable profile fields of a user. */
public interface UpdateUserUseCase {

    User updateUser(UUID id, UpdateUserCommand command);
}
