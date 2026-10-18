package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.TokenClaims;

/** Verify a bearer access token and extract its claims. */
public interface AccessTokenVerifier {

    /**
     * @throws co.sena.iam.domain.exception.UnauthenticatedException token absent, malformed or expired
     */
    TokenClaims verify(String accessToken);
}
