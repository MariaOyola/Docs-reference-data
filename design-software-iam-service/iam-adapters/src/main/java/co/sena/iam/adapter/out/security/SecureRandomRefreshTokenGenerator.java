package co.sena.iam.adapter.out.security;

import co.sena.iam.application.port.out.RefreshTokenGenerator;
import co.sena.iam.domain.model.RefreshTokenSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Opaque refresh token: 256 bits of entropy handed to the client, SHA-256 digest persisted.
 * A leaked {@code session.refresh_token} row therefore cannot be replayed.
 */
@Component
class SecureRandomRefreshTokenGenerator implements RefreshTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public RefreshTokenSecret generate() {
        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        return new RefreshTokenSecret(value, sha256Hex(value));
    }

    @Override
    public String hashOf(String value) {
        return sha256Hex(value);
    }

    static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JRE spec", e);
        }
    }
}
