package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Mint a signed access token. */
public interface AccessTokenIssuer {

    /**
     * @param featureScopes HU-IAM-010 E1 — the user's effective grants as {@code CODE:SCOPE_TYPE}
     *                      pairs ({@link co.sena.iam.domain.service.AuthorizationPolicy#effectiveFeatureScopes}),
     *                      so downstream services can authorize locally without calling iam.
     */
    String issue(
            User user, UserAccess access, List<String> featureScopes,
            UUID sessionId, Instant issuedAt, Instant expiresAt);
}
