package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import java.util.Objects;

/** Identity behind a valid access token, with its roles/features resolved. */
public record AuthenticatedIdentity(User user, UserAccess access) {

    public AuthenticatedIdentity {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(access, "access");
    }
}
