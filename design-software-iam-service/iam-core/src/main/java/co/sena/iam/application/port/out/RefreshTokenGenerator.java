package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.RefreshTokenSecret;

/** Generate and hash refresh tokens. */
public interface RefreshTokenGenerator {

    /** Mint a new refresh token (opaque value + its SHA-256 digest). */
    RefreshTokenSecret generate();

    /** Compute the same digest for a value received from the client (for lookup). */
    String hashOf(String value);
}
