package co.sena.iam.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** One allow-listed client for the Academic-to-Actors capacity check (HU-ACAD-10). */
@ConfigurationProperties(prefix = "iam.m2m.academic")
public record M2mClientProperties(String clientId, String clientSecret, long tokenTtlSeconds) {

    public M2mClientProperties {
        if (tokenTtlSeconds < 30 || tokenTtlSeconds > 900) {
            throw new IllegalArgumentException("iam.m2m.academic.token-ttl-seconds must be between 30 and 900");
        }
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public boolean matches(String candidateId, String candidateSecret) {
        if (!isConfigured() || candidateId == null || candidateSecret == null) {
            return false;
        }
        return MessageDigest.isEqual(
                        clientId.getBytes(StandardCharsets.UTF_8), candidateId.getBytes(StandardCharsets.UTF_8))
                && MessageDigest.isEqual(
                        clientSecret.getBytes(StandardCharsets.UTF_8), candidateSecret.getBytes(StandardCharsets.UTF_8));
    }
}
