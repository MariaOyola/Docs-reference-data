package co.sena.iam.domain.exception;

public final class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
