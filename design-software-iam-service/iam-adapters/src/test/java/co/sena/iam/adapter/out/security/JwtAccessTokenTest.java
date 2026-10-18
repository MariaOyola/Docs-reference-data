package co.sena.iam.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.domain.exception.UnauthenticatedException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.RoleAssignment;
import co.sena.iam.domain.model.TokenClaims;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The signed access token and its local verification. */
class JwtAccessTokenTest {

    private static final String ISSUER = "https://iam.test.sena.edu.co";
    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000042");
    private static final UUID SESSION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final List<String> FEATURE_SCOPES = List.of("SCH_VIEW_OWN:OWN_SCHEDULE");

    private static String privateKeyPem;
    private static String otherPrivateKeyPem;

    @BeforeAll
    static void generateKeys() throws NoSuchAlgorithmException {
        privateKeyPem = pkcs8Base64(newKeyPair());
        otherPrivateKeyPem = pkcs8Base64(newKeyPair());
    }

    private static KeyPair newKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String pkcs8Base64(KeyPair pair) {
        return Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    }

    private static RsaSigningKeys keys(String privateKey) {
        return new RsaSigningKeys(new JwtProperties(privateKey, ISSUER, "test-key", null, null));
    }

    private static User user() {
        return new User(USER_ID, "ana@sena.edu.co", "irrelevant", "Ana", "Ríos",
                ActorType.INSTRUCTOR, null, true, LockoutState.clear(), false, null);
    }

    private static UserAccess access() {
        return new UserAccess(
                List.of(new RoleAssignment("INSTRUCTOR", null)),
                List.of("schedule.read"),
                List.of("scheduling"));
    }

    @Test
    @DisplayName("a freshly issued token verifies back to its subject and session id")
    void issuedTokenVerifiesToItsSubjectAndSession() {
        RsaSigningKeys signingKeys = keys(privateKeyPem);
        Instant now = Instant.now();
        String token = new JwtAccessTokenIssuer(signingKeys)
                .issue(user(), access(), FEATURE_SCOPES, SESSION_ID, now, now.plus(TokenPolicy.ACCESS_TOKEN_TTL));

        TokenClaims claims = new JwtAccessTokenVerifier(signingKeys).verify(token);
        assertEquals(USER_ID, claims.userId());
        assertEquals(SESSION_ID, claims.sessionId());
        assertEquals(3, token.split("\\.").length, "a JWS has three parts");
    }

    @Test
    @DisplayName("(HU-IAM-010 E1) sub, roles, features (CODE:SCOPE_TYPE) and exp are stamped on the token")
    void stampsSubRolesFeatureScopesAndExpiry() {
        RsaSigningKeys signingKeys = keys(privateKeyPem);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TokenPolicy.ACCESS_TOKEN_TTL);

        String token = new JwtAccessTokenIssuer(signingKeys)
                .issue(user(), access(), FEATURE_SCOPES, SESSION_ID, now, expiresAt);

        Claims claims = Jwts.parser()
                .verifyWith(signingKeys.publicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(USER_ID.toString(), claims.getSubject());
        assertEquals(List.of("INSTRUCTOR"), claims.get("roles", List.class));
        assertEquals(FEATURE_SCOPES, claims.get("features", List.class));
        assertEquals(expiresAt.getEpochSecond(), claims.getExpiration().toInstant().getEpochSecond());
    }

    @Test
    @DisplayName("non-LEARNER tokens do not query Actors or include ficha_id")
    void doesNotLookupFichaForNonLearner() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/active-ficha", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        try {
            var actors = new co.sena.iam.adapter.out.client.ActorsActiveFichaClient(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/active-ficha/{user_id}",
                    "", "", "", "");
            Instant now = Instant.now();
            String token = new JwtAccessTokenIssuer(keys(privateKeyPem), actors)
                    .issue(user(), access(), FEATURE_SCOPES, SESSION_ID, now, now.plus(TokenPolicy.ACCESS_TOKEN_TTL));

            Claims claims = Jwts.parser().verifyWith(keys(privateKeyPem).publicKey()).build()
                    .parseSignedClaims(token).getPayload();
            assertEquals("INSTRUCTOR", claims.get("actor_type", String.class));
            assertTrue(claims.get("ficha_id") == null);
            assertEquals(0, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("a token signed by another key is rejected")
    void rejectsTokenSignedByAnotherKey() {
        Instant now = Instant.now();
        String forged = new JwtAccessTokenIssuer(keys(otherPrivateKeyPem))
                .issue(user(), access(), FEATURE_SCOPES, SESSION_ID, now, now.plus(TokenPolicy.ACCESS_TOKEN_TTL));
        JwtAccessTokenVerifier verifier = new JwtAccessTokenVerifier(keys(privateKeyPem));

        assertThrows(UnauthenticatedException.class, () -> verifier.verify(forged));
    }

    @Test
    @DisplayName("an expired token is rejected")
    void rejectsExpiredToken() {
        RsaSigningKeys signingKeys = keys(privateKeyPem);
        Instant longAgo = Instant.now().minusSeconds(3600);
        String expired = new JwtAccessTokenIssuer(signingKeys)
                .issue(user(), access(), FEATURE_SCOPES, SESSION_ID, longAgo, longAgo.plusSeconds(60));

        assertThrows(UnauthenticatedException.class,
                () -> new JwtAccessTokenVerifier(signingKeys).verify(expired));
    }

    @Test
    @DisplayName("a missing or blank token is rejected before any parsing")
    void rejectsMissingToken() {
        JwtAccessTokenVerifier verifier = new JwtAccessTokenVerifier(keys(privateKeyPem));

        assertThrows(UnauthenticatedException.class, () -> verifier.verify(null));
        assertThrows(UnauthenticatedException.class, () -> verifier.verify("  "));
    }

    @Test
    @DisplayName("PEM armour around the key is accepted just like the bare base64")
    void acceptsPemArmouredKey() {
        String pem = "-----BEGIN PRIVATE KEY-----\n" + privateKeyPem + "\n-----END PRIVATE KEY-----";

        assertEquals(keys(privateKeyPem).publicKey(), keys(pem).publicKey());
    }

    @Test
    @DisplayName("startup fails loudly when the signing key is missing or unusable")
    void refusesToStartWithoutAUsableKey() {
        assertThrows(IllegalStateException.class,
                () -> new RsaSigningKeys(new JwtProperties(null, ISSUER, null, null, null)));
        assertThrows(IllegalStateException.class,
                () -> new RsaSigningKeys(new JwtProperties("   ", ISSUER, null, null, null)));
        assertThrows(IllegalStateException.class,
                () -> new RsaSigningKeys(new JwtProperties(privateKeyPem, null, null, null, null)));
        assertThrows(IllegalStateException.class,
                () -> new RsaSigningKeys(new JwtProperties("not-a-key", ISSUER, null, null, null)));
    }

    @Test
    @DisplayName("(HU-IAM-010 E3) a configured previous key is exposed by its own kid, for JWKS during rotation")
    void exposesThePreviousKeyDuringRotation() throws NoSuchAlgorithmException {
        KeyPair previous = newKeyPair();
        String previousPublicKeyPem = Base64.getEncoder().encodeToString(previous.getPublic().getEncoded());

        RsaSigningKeys signingKeys = new RsaSigningKeys(
                new JwtProperties(privateKeyPem, ISSUER, "iam-rsa-2", previousPublicKeyPem, "iam-rsa-1"));

        assertEquals(previous.getPublic(), signingKeys.previousPublicKey().orElseThrow());
        assertEquals("iam-rsa-1", signingKeys.previousKeyId().orElseThrow());
    }

    @Test
    @DisplayName("(HU-IAM-010 E3) outside a rotation window, there is no previous key")
    void noPreviousKeyOutsideARotationWindow() {
        RsaSigningKeys signingKeys = keys(privateKeyPem);

        assertTrue(signingKeys.previousPublicKey().isEmpty());
        assertTrue(signingKeys.previousKeyId().isEmpty());
    }

    @Test
    @DisplayName("(HU-IAM-010 E3) a previous public key without its kid fails startup")
    void previousKeyWithoutKidFailsStartup() throws NoSuchAlgorithmException {
        String previousPublicKeyPem = Base64.getEncoder().encodeToString(newKeyPair().getPublic().getEncoded());

        assertThrows(IllegalStateException.class, () -> new RsaSigningKeys(
                new JwtProperties(privateKeyPem, ISSUER, "iam-rsa-2", previousPublicKeyPem, null)));
    }

    @Test
    @DisplayName("each refresh token is unique and only its digest is meant to be stored")
    void refreshTokensAreUniqueAndHashed() {
        SecureRandomRefreshTokenGenerator generator = new SecureRandomRefreshTokenGenerator();

        var first = generator.generate();
        var second = generator.generate();

        assertNotEquals(first.value(), second.value());
        assertNotEquals(first.value(), first.hash());
        assertEquals(64, first.hash().length(), "SHA-256 as hex");
        assertEquals(first.hash(), SecureRandomRefreshTokenGenerator.sha256Hex(first.value()));
        assertTrue(first.value().length() >= 43, "256 bits of entropy in base64url");
    }
}
