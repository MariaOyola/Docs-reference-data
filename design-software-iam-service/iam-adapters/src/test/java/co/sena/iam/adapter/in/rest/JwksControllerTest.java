package co.sena.iam.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import co.sena.iam.adapter.out.security.JwtProperties;
import co.sena.iam.adapter.out.security.RsaSigningKeys;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-010 E3 — the public key(s) as a standard JWKS, for local verification. */
class JwksControllerTest {

    private static final String ISSUER = "https://iam.test.sena.edu.co";

    @Test
    @DisplayName("publishes the current key as an RSA JWK keyed by its kid")
    void publishesCurrentKeyAsRsaJwk() throws NoSuchAlgorithmException {
        String privateKeyPem = pkcs8Base64(newKeyPair());
        RsaSigningKeys keys = new RsaSigningKeys(
                new JwtProperties(privateKeyPem, ISSUER, "iam-rsa-2026", null, null));

        Map<String, Object> body = new JwksController(keys).jwks();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jwks = (List<Map<String, Object>>) body.get("keys");
        assertThat(jwks).hasSize(1);
        Map<String, Object> jwk = jwks.get(0);
        assertThat(jwk.get("kty")).isEqualTo("RSA");
        assertThat(jwk.get("use")).isEqualTo("sig");
        assertThat(jwk.get("alg")).isEqualTo("RS256");
        assertThat(jwk.get("kid")).isEqualTo("iam-rsa-2026");
        assertThat((String) jwk.get("n")).isNotBlank();
        assertThat((String) jwk.get("e")).isNotBlank();
        // JWK ints are base64url: no '+', '/' or '=' padding.
        assertThat((String) jwk.get("n")).doesNotContain("+", "/", "=");
    }

    @Test
    @DisplayName("during a rotation window, both the current and the previous key are published under their own kid")
    void publishesBothKeysDuringRotation() throws NoSuchAlgorithmException {
        String currentPrivateKeyPem = pkcs8Base64(newKeyPair());
        String previousPublicKeyPem = x509Base64(newKeyPair());
        RsaSigningKeys keys = new RsaSigningKeys(new JwtProperties(
                currentPrivateKeyPem, ISSUER, "iam-rsa-2", previousPublicKeyPem, "iam-rsa-1"));

        Map<String, Object> body = new JwksController(keys).jwks();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jwks = (List<Map<String, Object>>) body.get("keys");
        assertThat(jwks).hasSize(2);
        assertThat(jwks.stream().map(jwk -> jwk.get("kid"))).containsExactly("iam-rsa-2", "iam-rsa-1");
    }

    private static KeyPair newKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String pkcs8Base64(KeyPair pair) {
        return Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    }

    private static String x509Base64(KeyPair pair) {
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }
}
