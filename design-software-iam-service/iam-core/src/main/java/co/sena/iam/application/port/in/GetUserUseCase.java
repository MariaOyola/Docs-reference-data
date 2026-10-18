package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.User;
import java.util.UUID;

/** E2: returns a single user by id. */
public interface GetUserUseCase {

    User getUser(UUID id);
}
