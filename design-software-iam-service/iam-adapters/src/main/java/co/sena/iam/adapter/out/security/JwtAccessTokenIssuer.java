package co.sena.iam.adapter.out.security;

import co.sena.iam.application.port.out.AccessTokenIssuer;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import co.sena.iam.adapter.out.client.ActorsActiveFichaClient;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * RS256 access token, as declared by the contract ({@code bearerAuth}: JWT RS256 issued by
 * iam-service and verified locally through JWKS). Embeds {@code sid} (session ID) so the
 * logout endpoint can identify and revoke the specific session.
 *
 * <p>HU-IAM-010 E1 — {@code features} carries {@code CODE:SCOPE_TYPE} pairs (see
 * {@code authentication.md}), so a downstream service can authorize a request against its
 * own feature+scope requirement without calling back to iam.
 */
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final RsaSigningKeys keys;
    private final ActorsActiveFichaClient actors;

    public JwtAccessTokenIssuer(RsaSigningKeys keys, ActorsActiveFichaClient actors) {
        this.keys = keys;
        this.actors = actors;
    }

    JwtAccessTokenIssuer(RsaSigningKeys keys) {
        this(keys, null);
    }

    @Override
    public String issue(
            User user, UserAccess access, List<String> featureScopes,
            UUID sessionId, Instant issuedAt, Instant expiresAt) {
        var builder = Jwts.builder()
                .header().keyId(keys.keyId()).and()
                .id(UUID.randomUUID().toString())
                .issuer(keys.issuer())
                .subject(user.id().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("sid", sessionId.toString())
                .claim("email", user.email())
                .claim("name", user.fullName())
                .claim("actor_type", user.actorType().name())
                .claim("actor_id", user.actorId() == null ? null : user.actorId().toString())
                .claim("roles", access.roleNames())
                .claim("features", featureScopes)
                .claim("training_center_id", trainingCenterId(access));
        if (actors != null && "LEARNER".equals(user.actorType().name())) {
            actors.findActiveFicha(user.id()).ifPresent(ficha -> builder.claim("ficha_id", ficha.toString()));
        }
        return builder.signWith(keys.privateKey(), Jwts.SIG.RS256).compact();
    }

    private static String trainingCenterId(UserAccess access) {
        UUID id = access.primaryTrainingCenterId();
        return id == null ? null : id.toString();
    }
}
