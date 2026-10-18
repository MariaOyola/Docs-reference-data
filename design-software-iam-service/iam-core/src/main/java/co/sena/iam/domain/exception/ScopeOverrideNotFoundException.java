package co.sena.iam.domain.exception;

import java.util.UUID;

/** No scope override with the given id exists for the target user. */
public final class ScopeOverrideNotFoundException extends RuntimeException {

    public ScopeOverrideNotFoundException(UUID overrideId) {
        super("Scope override not found: " + overrideId);
    }
}
