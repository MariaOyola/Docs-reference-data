package co.sena.iam.domain.exception;

import java.util.UUID;

/** Session not found for the given user → 404. */
public final class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
