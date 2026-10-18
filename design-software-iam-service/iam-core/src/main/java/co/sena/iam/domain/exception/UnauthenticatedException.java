package co.sena.iam.domain.exception;

/** No usable access token: missing, malformed, expired, or its subject no longer exists. */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }

    public UnauthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
