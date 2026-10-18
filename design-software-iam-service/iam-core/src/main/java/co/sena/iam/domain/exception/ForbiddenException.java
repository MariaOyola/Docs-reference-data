package co.sena.iam.domain.exception;

/** The caller is authenticated but lacks the feature/scope grant the operation requires. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
