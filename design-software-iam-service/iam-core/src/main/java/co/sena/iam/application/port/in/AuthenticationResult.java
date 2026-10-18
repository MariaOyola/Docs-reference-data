package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import java.util.Objects;

/**
 * Outcome of a successful authentication: the tokens plus everything the REST adapter
 * needs to fill {@code TokenResponse}.
 *
 * @param expiresInSeconds lifetime of {@code accessToken}
 */
public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        User user,
        UserAccess access) {

    public AuthenticationResult {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(access, "access");
    }
}
