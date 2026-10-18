package co.sena.iam.adapter.out.security;

import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Issues a short-lived, least-privilege token for the single Academic service client. */
@Component
@EnableConfigurationProperties(M2mClientProperties.class)
public class M2mAccessTokenIssuer {

    public static final List<String> ACADEMIC_FEATURES = List.of(
            "ACT_INTERNAL_FICHA_LEARNER_COUNT:GLOBAL",
            "ACADEMIC_FICHA_VIEW:GLOBAL");

    private final RsaSigningKeys keys;
    private final M2mClientProperties client;
    private final Clock clock;

    public M2mAccessTokenIssuer(RsaSigningKeys keys, M2mClientProperties client, Clock clock) {
        this.keys = keys;
        this.client = client;
        this.clock = clock;
    }

    public String issue() {
        Instant issuedAt = clock.instant();
        UUID serviceSubject = UUID.nameUUIDFromBytes(
                ("service:" + client.clientId()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Jwts.builder()
                .header().keyId(keys.keyId()).and()
                .id(UUID.randomUUID().toString())
                .issuer(keys.issuer())
                .subject(serviceSubject.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(client.tokenTtlSeconds())))
                .claim("client_id", client.clientId())
                .claim("token_use", "service")
                .claim("actor_type", "SERVICE")
                .claim("roles", List.of("ACADEMIC_MANAGEMENT_SERVICE"))
                .claim("features", ACADEMIC_FEATURES)
                .signWith(keys.privateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
