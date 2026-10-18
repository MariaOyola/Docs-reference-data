package co.sena.iam.domain.exception;

/**
 * Email unknown, password wrong, or the account is not usable. Deliberately one single
 * exception so the API answer cannot be used to probe which emails exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
