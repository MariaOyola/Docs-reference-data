package co.sena.iam.application.port.in;

import java.util.Objects;

/**
 * Credentials plus the request context that ends up in the audit trail.
 *
 * @param ipAddress  nullable
 * @param userAgent  nullable
 * @param deviceHint nullable
 */
public record LoginCommand(
        String email,
        String password,
        String ipAddress,
        String userAgent,
        String deviceHint) {

    public LoginCommand {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(password, "password");
    }

    public static LoginCommand of(String email, String password) {
        return new LoginCommand(email, password, null, null, null);
    }
}
