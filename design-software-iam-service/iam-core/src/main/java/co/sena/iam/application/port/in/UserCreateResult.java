package co.sena.iam.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record UserCreateResult(UUID id, String email, String temporaryPassword) {

    public UserCreateResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(temporaryPassword, "temporaryPassword");
    }
}
