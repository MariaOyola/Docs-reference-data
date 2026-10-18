package co.sena.iam.adapter.out.security;

import co.sena.iam.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt verification of {@code identity.user.password_hash}. */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return encoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException malformedHash) {
            // A stored value that is not a BCrypt hash (e.g. a placeholder seed) is not a match.
            return false;
        }
    }
}
