package co.sena.iam.application.port.out;

/** Outbound port for password hashing and verification (BCrypt in the adapter). */
public interface PasswordHasher {

    /** Produces a salted hash of {@code rawPassword} suitable for storage. */
    String hash(String rawPassword);

    /** Constant-time comparison of a raw password against a stored hash. */
    boolean matches(String rawPassword, String passwordHash);
}
