package co.sena.iam.adapter.in.rest;

import co.sena.iam.adapter.out.security.M2mAccessTokenIssuer;
import co.sena.iam.adapter.out.security.M2mClientProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OAuth2 client-credentials endpoint for Academic's internal Actors count call. */
@RestController
@RequestMapping("/api/v1/auth")
class M2mTokenController {

    private final M2mClientProperties client;
    private final M2mAccessTokenIssuer issuer;

    M2mTokenController(M2mClientProperties client, M2mAccessTokenIssuer issuer) {
        this.client = client;
        this.issuer = issuer;
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> token(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "grant_type", required = false) String grantType) {
        if (!"client_credentials".equals(grantType)) {
            return oauthError(HttpStatus.BAD_REQUEST, "unsupported_grant_type");
        }

        String[] credentials = basicCredentials(authorization);
        if (credentials == null || !client.matches(credentials[0], credentials[1])) {
            return oauthError(HttpStatus.UNAUTHORIZED, "invalid_client");
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(new TokenResponse(issuer.issue(), "Bearer", client.tokenTtlSeconds(),
                        String.join(" ", M2mAccessTokenIssuer.ACADEMIC_FEATURES)));
    }

    private static String[] basicCredentials(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()),
                    StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator <= 0) {
                return null;
            }
            return new String[] {decoded.substring(0, separator), decoded.substring(separator + 1)};
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ResponseEntity<OAuthError> oauthError(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(new OAuthError(code, "Client credentials grant was rejected."));
    }

    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("scope") String scope) {
    }

    record OAuthError(
            @JsonProperty("error") String error,
            @JsonProperty("error_description") String errorDescription) {
    }
}
