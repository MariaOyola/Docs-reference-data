package co.sena.iam.adapter.in.rest;

import co.sena.iam.adapter.out.security.RsaSigningKeys;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-IAM-010 E2/E3 — publishes iam-service's RSA public key(s) as a standard JWKS
 * ({@code RFC 7517}), so the edge (Traefik, ADR-007) and every downstream service can verify
 * the access token's signature locally, without calling iam. No {@code Authorization} header
 * is required: a client cannot bootstrap local verification by first proving it can verify.
 *
 * <p>During a key rotation window, both the current and the previous key are published
 * (each under its own {@code kid}, see {@code authentication.md}), so tokens signed before
 * the rotation keep verifying until they expire (access tokens: 15 min).
 */
@RestController
@RequestMapping("/api/v1/auth/.well-known")
class JwksController {

    private final RsaSigningKeys keys;

    JwksController(RsaSigningKeys keys) {
        this.keys = keys;
    }

    @GetMapping("/jwks.json")
    Map<String, Object> jwks() {
        List<Map<String, Object>> jwks = new ArrayList<>();
        jwks.add(rsaJwk(keys.keyId(), (RSAPublicKey) keys.publicKey()));
        keys.previousKeyId().ifPresent(previousKeyId ->
                jwks.add(rsaJwk(previousKeyId, (RSAPublicKey) keys.previousPublicKey().orElseThrow())));
        return Map.of("keys", jwks);
    }

    private static Map<String, Object> rsaJwk(String keyId, RSAPublicKey key) {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", keyId);
        jwk.put("n", base64UrlUnsigned(key.getModulus()));
        jwk.put("e", base64UrlUnsigned(key.getPublicExponent()));
        return jwk;
    }

    /** JWK integers are base64url, no padding, big-endian, without a leading sign byte. */
    private static String base64UrlUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
