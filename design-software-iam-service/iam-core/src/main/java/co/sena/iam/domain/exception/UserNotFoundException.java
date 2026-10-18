package co.sena.iam.domain.exception;

import java.util.UUID;

public final class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("User not found: " + id);
    }
}
