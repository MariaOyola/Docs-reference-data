package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Flat event envelope to be staged in {@code identity.outbox}.
 * Matches the {@code event-envelope.schema.json} from shared-contracts.
 *
 * @param payload inner business data (nested inside the envelope)
 */
public record OutboxEvent(
        UUID eventId,
        String eventType,
        String version,
        Instant timestamp,
        String sourceService,
        UUID correlationId,
        Map<String, Object> payload) {

    public OutboxEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(sourceService, "sourceService");
        Objects.requireNonNull(payload, "payload");
    }

    public static OutboxEvent of(String eventType, Map<String, Object> payload) {
        return new OutboxEvent(
                UUID.randomUUID(),
                eventType,
                "1",
                Instant.now(),
                "iam",
                null,
                payload);
    }
}
