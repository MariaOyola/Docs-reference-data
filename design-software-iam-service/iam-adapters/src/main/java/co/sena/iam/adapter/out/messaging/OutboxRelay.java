package co.sena.iam.adapter.out.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-IAM-007 — transactional outbox relay for {@code identity.outbox}.
 *
 * <p>Polls unpublished rows (oldest first) with {@code FOR UPDATE SKIP LOCKED} so several
 * instances can run concurrently, publishes each to the {@code iam-events} topic exchange,
 * and marks {@code published_at} — all in one transaction. If a publish fails partway
 * through, the transaction rolls back: rows already sent to the broker in this batch stay
 * unpublished in the DB and are retried (and re-sent) on the next poll. That is the
 * deliberate at-least-once trade-off — consumers dedup by {@code event_id} (which also
 * travels as the AMQP message id).
 */
public class OutboxRelay {

    /**
     * The row's {@code payload} column already holds a full event envelope (staged by the
     * HU-IAM-005 use cases), but with the business data under {@code data}. The wire contract
     * (shared-contracts {@code event-envelope.schema.json}) requires that key to be
     * {@code payload}, so the relay rekeys it on the way out. Kept defensive: an envelope that
     * already uses {@code payload} is published unchanged.
     */
    private static final String STAGED_DATA_KEY = "data";
    private static final String ENVELOPE_PAYLOAD_KEY = "payload";
    private static final String CORRELATION_ID_KEY = "correlation_id";

    private static final RowMapper<StagedRow> ROW_MAPPER = (rs, rowNum) -> new StagedRow(
            rs.getString("id"),
            rs.getString("event_id"),
            rs.getString("event_type"),
            rs.getString("payload"));

    private final JdbcTemplate jdbc;
    private final EventPublisher publisher;
    private final ObjectMapper mapper;
    private final int batchSize;

    public OutboxRelay(JdbcTemplate jdbc, EventPublisher publisher, ObjectMapper mapper, int batchSize) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive, was " + batchSize);
        }
        this.batchSize = batchSize;
    }

    /**
     * Publishes up to {@code batchSize} unpublished rows and marks each published, in one
     * transaction.
     *
     * @return the number of rows published in this pass
     */
    @Transactional
    public int pollAndPublish() {
        return publish(fetchUnpublished());
    }

    /**
     * Locks and returns the oldest unpublished rows. {@code FOR UPDATE SKIP LOCKED} lets
     * several relay instances share the table without publishing the same row twice.
     */
    List<StagedRow> fetchUnpublished() {
        return jdbc.query(
                """
                SELECT id::text AS id, event_id::text AS event_id, event_type, payload::text AS payload
                FROM identity.outbox
                WHERE published_at IS NULL
                ORDER BY created_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """,
                ROW_MAPPER,
                batchSize);
    }

    /** Publishes each row and marks it published; a publish failure aborts the batch (rollback). */
    int publish(List<StagedRow> staged) {
        int published = 0;
        for (StagedRow row : staged) {
            publisher.publish(row.eventType(), row.eventId(), toWireEnvelope(row.payload()));
            jdbc.update("UPDATE identity.outbox SET published_at = now() WHERE id = ?::uuid", row.id());
            published++;
        }
        return published;
    }

    /**
     * Rebuilds the stored envelope into a shared-contracts-conformant one: the business data
     * moves from {@code data} to {@code payload}, and a null {@code correlation_id} (optional,
     * uuid when present) is dropped. All other envelope fields are preserved verbatim.
     */
    byte[] toWireEnvelope(String storedJson) {
        try {
            JsonNode parsed = mapper.readTree(storedJson);
            if (!(parsed instanceof ObjectNode envelope)) {
                throw new IllegalStateException("outbox payload is not a JSON object: " + storedJson);
            }
            if (!envelope.has(ENVELOPE_PAYLOAD_KEY)) {
                JsonNode data = envelope.has(STAGED_DATA_KEY)
                        ? envelope.remove(STAGED_DATA_KEY)
                        : mapper.createObjectNode();
                envelope.set(ENVELOPE_PAYLOAD_KEY, data);
            } else {
                envelope.remove(STAGED_DATA_KEY);
            }
            if (envelope.path(CORRELATION_ID_KEY).isNull()) {
                envelope.remove(CORRELATION_ID_KEY);
            }
            return mapper.writeValueAsBytes(envelope);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to read outbox payload", e);
        }
    }

    /** A single {@code identity.outbox} row, projected to the fields the relay needs. */
    record StagedRow(String id, String eventId, String eventType, String payload) {
    }
}
