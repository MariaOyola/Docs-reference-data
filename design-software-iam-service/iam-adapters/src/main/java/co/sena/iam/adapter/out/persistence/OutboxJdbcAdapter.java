package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.domain.model.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stages domain events into {@code identity.outbox} in the caller's transaction. */
@Repository
class OutboxJdbcAdapter implements OutboxPort {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    OutboxJdbcAdapter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public void stage(OutboxEvent event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event_id", event.eventId().toString());
        envelope.put("event_type", event.eventType());
        envelope.put("version", event.version());
        envelope.put("timestamp", event.timestamp().toString());
        envelope.put("source_service", event.sourceService());
        envelope.put("correlation_id", event.correlationId() != null ? event.correlationId().toString() : null);
        envelope.put("data", event.payload());

        try {
            String json = mapper.writeValueAsString(envelope);
            jdbc.update(
                    "INSERT INTO identity.outbox (event_id, event_type, payload) VALUES (?, ?, CAST(? AS jsonb))",
                    event.eventId(), event.eventType(), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
