package co.sena.iam.adapter.out.security;

import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.domain.exception.UnauthenticatedException;
import co.sena.iam.domain.model.TokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Local verification of the tokens this service issued (signature, issuer and expiry). */
@Component
class JwtAccessTokenVerifier implements AccessTokenVerifier {

    private final RsaSigningKeys keys;

    JwtAccessTokenVerifier(RsaSigningKeys keys) {
        this.keys = keys;
    }

    @Override
    public TokenClaims verify(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new UnauthenticatedException("Missing access token");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keys.publicKey())
                    .requireIssuer(keys.issuer())
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            UUID sessionId = UUID.fromString(claims.get("sid", String.class));
            return new TokenClaims(userId, sessionId);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthenticatedException("Access token rejected", e);
        }
    }
}
