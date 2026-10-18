package co.sena.iam.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * HU-IAM-007 unit tests for the outbox relay's publish-and-mark loop and its envelope
 * reconciliation. The DB polling (Postgres-specific SQL) is covered by
 * {@code OutboxRelayPostgresIT}; here the {@link JdbcTemplate} is a mock and the broker is a
 * recording double, so the pure orchestration and mapping are exercised in isolation.
 */
class OutboxRelayTest {

    private static final String MARK_SQL_FRAGMENT = "published_at = now()";

    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final OutboxRelay relay = new OutboxRelay(jdbc, publisher, mapper, 100);

    @Test
    void publishesEachUnpublishedRowThenMarksItPublished() {
        OutboxRelay.StagedRow first = row("11111111-1111-1111-1111-111111111111",
                "aaaaaaaa-0000-0000-0000-000000000001", "iam.user.created", "{\"user_id\":\"u1\"}");
        OutboxRelay.StagedRow second = row("22222222-2222-2222-2222-222222222222",
                "aaaaaaaa-0000-0000-0000-000000000002", "iam.session.started", "{\"user_id\":\"u2\"}");

        int published = relay.publish(List.of(first, second));

        assertThat(published).isEqualTo(2);
        // idempotency: routing key is the event type, the AMQP message id is the event id.
        assertThat(publisher.messages).hasSize(2);
        assertThat(publisher.messages.get(0).routingKey()).isEqualTo("iam.user.created");
        assertThat(publisher.messages.get(0).messageId()).isEqualTo("aaaaaaaa-0000-0000-0000-000000000001");
        assertThat(publisher.messages.get(1).routingKey()).isEqualTo("iam.session.started");
        // both rows marked published_at, by id.
        verify(jdbc).update(anyString(), eq("11111111-1111-1111-1111-111111111111"));
        verify(jdbc).update(anyString(), eq("22222222-2222-2222-2222-222222222222"));
        verify(jdbc, times(2)).update(anyString(), anyString());
    }

    @Test
    void publishesNothingForAnEmptyBatch() {
        int published = relay.publish(List.of());

        assertThat(published).isZero();
        assertThat(publisher.messages).isEmpty();
        verify(jdbc, never()).update(anyString(), anyString());
    }

    @Test
    void abortsTheBatchWhenAPublishFails() {
        publisher.failOnRoutingKey = "iam.session.started";
        OutboxRelay.StagedRow ok = row("11111111-1111-1111-1111-111111111111",
                "aaaaaaaa-0000-0000-0000-000000000001", "iam.user.created", "{\"user_id\":\"u1\"}");
        OutboxRelay.StagedRow boom = row("22222222-2222-2222-2222-222222222222",
                "aaaaaaaa-0000-0000-0000-000000000002", "iam.session.started", "{\"user_id\":\"u2\"}");

        assertThatThrownBy(() -> relay.publish(List.of(ok, boom)))
                .isInstanceOf(RuntimeException.class);

        // the first row was marked; the failing row (and anything after it) was not — so the
        // surrounding transaction rolls the mark back and the whole batch is retried next tick.
        verify(jdbc, times(1)).update(anyString(), anyString());
        verify(jdbc).update(anyString(), eq("11111111-1111-1111-1111-111111111111"));
        verify(jdbc, never()).update(anyString(), eq("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void rekeysStagedDataToEnvelopePayloadOnTheWire() throws Exception {
        String stored = "{"
                + "\"event_id\":\"aaaaaaaa-0000-0000-0000-000000000001\","
                + "\"event_type\":\"iam.user.created\","
                + "\"version\":\"1\",\"timestamp\":\"2026-08-26T00:00:00Z\","
                + "\"source_service\":\"iam\",\"correlation_id\":null,"
                + "\"data\":{\"user_id\":\"u1\",\"email\":\"x@sena.edu.co\"}}";

        JsonNode wire = mapper.readTree(relay.toWireEnvelope(stored));

        assertThat(wire.has("data")).isFalse();
        assertThat(wire.has("payload")).isTrue();
        assertThat(wire.get("payload").get("user_id").asText()).isEqualTo("u1");
        assertThat(wire.get("payload").get("email").asText()).isEqualTo("x@sena.edu.co");
        // untouched envelope fields survive.
        assertThat(wire.get("event_id").asText()).isEqualTo("aaaaaaaa-0000-0000-0000-000000000001");
        assertThat(wire.get("event_type").asText()).isEqualTo("iam.user.created");
        assertThat(wire.get("source_service").asText()).isEqualTo("iam");
        // a null correlation_id is dropped (schema: uuid string when present).
        assertThat(wire.has("correlation_id")).isFalse();
    }

    @Test
    void keepsAPresentCorrelationIdAndAnAlreadyPayloadKeyedEnvelope() throws Exception {
        String stored = "{"
                + "\"event_id\":\"aaaaaaaa-0000-0000-0000-000000000009\","
                + "\"event_type\":\"iam.role.assigned\",\"version\":\"1\","
                + "\"timestamp\":\"2026-08-26T00:00:00Z\",\"source_service\":\"iam\","
                + "\"correlation_id\":\"bbbbbbbb-0000-0000-0000-000000000001\","
                + "\"payload\":{\"role_name\":\"INSTRUCTOR\"}}";

        JsonNode wire = mapper.readTree(relay.toWireEnvelope(stored));

        assertThat(wire.get("payload").get("role_name").asText()).isEqualTo("INSTRUCTOR");
        assertThat(wire.get("correlation_id").asText()).isEqualTo("bbbbbbbb-0000-0000-0000-000000000001");
        assertThat(wire.has("data")).isFalse();
    }

    @Test
    void rejectsANonObjectPayload() {
        assertThatThrownBy(() -> relay.toWireEnvelope("[1,2,3]"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsANonPositiveBatchSize() {
        assertThatThrownBy(() -> new OutboxRelay(jdbc, publisher, mapper, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static OutboxRelay.StagedRow row(String id, String eventId, String eventType, String data) {
        String envelope = "{\"event_id\":\"" + eventId + "\",\"event_type\":\"" + eventType
                + "\",\"version\":\"1\",\"timestamp\":\"2026-08-26T00:00:00Z\",\"source_service\":\"iam\","
                + "\"correlation_id\":null,\"data\":" + data + "}";
        return new OutboxRelay.StagedRow(id, eventId, eventType, envelope);
    }

    /** Records what the relay hands the broker, and can be told to fail on a routing key. */
    private static final class RecordingPublisher implements EventPublisher {

        private record Sent(String routingKey, String messageId, byte[] body) {
        }

        private final List<Sent> messages = new ArrayList<>();
        private String failOnRoutingKey;

        @Override
        public void publish(String routingKey, String messageId, byte[] body) {
            if (routingKey.equals(failOnRoutingKey)) {
                throw new IllegalStateException("broker down for " + routingKey);
            }
            messages.add(new Sent(routingKey, messageId, body));
        }
    }
}
