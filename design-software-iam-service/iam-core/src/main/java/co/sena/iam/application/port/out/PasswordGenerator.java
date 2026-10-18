package co.sena.iam.application.port.out;

/** Generates a secure random temporary password for a new user account. */
public interface PasswordGenerator {

    String generate();
}
