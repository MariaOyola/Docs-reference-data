package co.sena.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Claims extracted from a verified access token. */
public record TokenClaims(UUID userId, UUID sessionId) {

    public TokenClaims {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(sessionId, "sessionId");
    }
}
