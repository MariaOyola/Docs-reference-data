package co.sena.iam.adapter.out.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Internal mTLS lookup used only while issuing a LEARNER token. */
@Component
public final class ActorsActiveFichaClient {
    private static final Logger LOG = LoggerFactory.getLogger(ActorsActiveFichaClient.class);
    private final String endpoint;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public ActorsActiveFichaClient(
            @Value("${iam.actors.active-ficha-url:}") String endpoint,
            @Value("${iam.actors.mtls.key-store:}") String keyStore,
            @Value("${iam.actors.mtls.key-store-password:}") String keyStorePassword,
            @Value("${iam.actors.mtls.trust-store:}") String trustStore,
        @Value("${iam.actors.mtls.trust-store-password:}") String trustStorePassword) {
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        boolean https = this.endpoint.startsWith("https://");
        this.client = HttpClient.newBuilder()
                .sslContext(sslContext(keyStore, keyStorePassword, trustStore, trustStorePassword, https))
                .build();
    }

    public Optional<UUID> findActiveFicha(UUID userId) {
        if (endpoint.isBlank()) return Optional.empty();
        try {
            URI uri = URI.create(endpoint.replace("{user_id}", userId.toString()));
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(uri).GET().header("Accept", "application/json").build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().isBlank()) {
                LOG.warn("Actors active ficha lookup returned status {} for user {}", response.statusCode(), userId);
                return Optional.empty();
            }
            var json = mapper.readTree(response.body());
            String ficha = json.hasNonNull("ficha_id")
                    ? json.get("ficha_id").asText()
                    : json.hasNonNull("fichaId") ? json.get("fichaId").asText() : null;
            return ficha == null || ficha.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(ficha));
        } catch (Exception ex) {
            LOG.warn("Actors active ficha lookup failed for user {}: {}", userId, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static SSLContext sslContext(
            String keyStore, String keyPassword, String trustStore, String trustPassword, boolean requireMtls) {
        if (keyStore.isBlank() && trustStore.isBlank()) {
            if (requireMtls) {
                throw new IllegalStateException("IAM Actors HTTPS integration requires key and trust stores");
            }
            try { return SSLContext.getDefault(); } catch (Exception e) { throw new IllegalStateException("Unable to initialize TLS", e); }
        }
        if (keyStore.isBlank() || trustStore.isBlank()) throw new IllegalStateException("IAM Actors mTLS requires both key and trust stores");
        try {
            KeyStore key = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(Path.of(keyStore))) { key.load(in, keyPassword.toCharArray()); }
            KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            km.init(key, keyPassword.toCharArray());
            KeyStore trust = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(Path.of(trustStore))) { trust.load(in, trustPassword.toCharArray()); }
            TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tm.init(trust);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(km.getKeyManagers(), tm.getTrustManagers(), null);
            return context;
        } catch (Exception e) { throw new IllegalStateException("Unable to initialize IAM Actors mTLS", e); }
    }

}
