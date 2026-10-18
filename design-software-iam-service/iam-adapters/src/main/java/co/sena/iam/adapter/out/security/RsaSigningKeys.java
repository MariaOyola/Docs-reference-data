package co.sena.iam.adapter.out.security;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The RS256 key pair used to sign and verify access tokens, plus (HU-IAM-010 E3) the
 * previous key's public half during a rotation window, so the {@code /jwks.json} endpoint
 * can publish both and downstream services keep verifying tokens signed before the rotation
 * until they expire (access tokens are short-lived: 15 min).
 *
 * <p>Only the private key is configured; its public half is derived from it. The previous
 * key, if any, is configured as a bare public key since its private half must no longer
 * exist anywhere once retired.
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class RsaSigningKeys {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String issuer;
    private final String keyId;
    private final PublicKey previousPublicKey;
    private final String previousKeyId;

    public RsaSigningKeys(JwtProperties properties) {
        if (properties.privateKey() == null || properties.privateKey().isBlank()) {
            throw new IllegalStateException(
                    "iam.jwt.private-key is required (set IAM_JWT_PRIVATE_KEY); refusing to start without a signing key");
        }
        if (properties.issuer() == null || properties.issuer().isBlank()) {
            throw new IllegalStateException("iam.jwt.issuer is required (set IAM_JWT_ISSUER)");
        }
        this.privateKey = readPrivateKey(properties.privateKey());
        this.publicKey = derivePublicKey(this.privateKey);
        this.issuer = properties.issuer();
        this.keyId = properties.keyId() == null || properties.keyId().isBlank() ? "iam-rsa-1" : properties.keyId();

        boolean hasPreviousKey = properties.previousPublicKey() != null && !properties.previousPublicKey().isBlank();
        if (hasPreviousKey) {
            if (properties.previousKeyId() == null || properties.previousKeyId().isBlank()) {
                throw new IllegalStateException(
                        "iam.jwt.previous-key-id is required when iam.jwt.previous-public-key is set");
            }
            this.previousPublicKey = readPublicKey(properties.previousPublicKey());
            this.previousKeyId = properties.previousKeyId();
        } else {
            this.previousPublicKey = null;
            this.previousKeyId = null;
        }
    }

    PrivateKey privateKey() {
        return privateKey;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    String issuer() {
        return issuer;
    }

    public String keyId() {
        return keyId;
    }

    public Optional<PublicKey> previousPublicKey() {
        return Optional.ofNullable(previousPublicKey);
    }

    public Optional<String> previousKeyId() {
        return Optional.ofNullable(previousKeyId);
    }

    /** Accepts a PEM block or the bare base64 body, with or without line breaks. */
    private static PrivateKey readPrivateKey(String configured) {
        String base64 = configured
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(
                    "iam.jwt.private-key is not a PKCS#8 RSA private key (PEM or base64)", e);
        }
    }

    /** Accepts a PEM block or the bare base64 body, with or without line breaks. */
    private static PublicKey readPublicKey(String configured) {
        String base64 = configured
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(
                    "iam.jwt.previous-public-key is not an X.509 RSA public key (PEM or base64)", e);
        }
    }

    private static PublicKey derivePublicKey(PrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
            throw new IllegalStateException(
                    "iam.jwt.private-key must be an RSA CRT key so the public key can be derived");
        }
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cannot derive the RSA public key", e);
        }
    }
}
