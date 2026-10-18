package co.sena.iam.domain.model;

import java.util.Objects;

/**
 * A freshly minted refresh token: the opaque value handed to the client and the digest
 * that is the only thing ever stored ({@code session.refresh_token.token_hash}).
 */
public record RefreshTokenSecret(String value, String hash) {

    public RefreshTokenSecret {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(hash, "hash");
    }
}
