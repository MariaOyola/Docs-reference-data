package co.sena.iam.adapter.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Signing configuration. Bound from {@code IAM_JWT_*} environment variables; there is no
 * default for the key on purpose, so a missing secret stops the service at startup instead
 * of silently signing with something well known.
 *
 * <p>HU-IAM-010 E3 — {@code previousPublicKey}/{@code previousKeyId} are optional and only
 * set during a key rotation window: the retiring key's public half stays published in the
 * JWKS response (keyed by its own {@code kid}) so tokens it already signed keep verifying
 * locally until they expire, while new tokens sign with {@code privateKey}/{@code keyId}.
 *
 * @param privateKey        RSA private key, PKCS#8, PEM or bare base64
 * @param issuer            {@code iss} claim, also required on verification
 * @param keyId             {@code kid} header, so keys can be rotated and published via JWKS
 * @param previousPublicKey RSA public key of the key being rotated out, X.509, PEM or bare
 *                          base64; {@code null} outside a rotation window
 * @param previousKeyId     {@code kid} of {@code previousPublicKey}; required when it is set
 */
@ConfigurationProperties(prefix = "iam.jwt")
public record JwtProperties(
        String privateKey, String issuer, String keyId,
        String previousPublicKey, String previousKeyId) {
}
