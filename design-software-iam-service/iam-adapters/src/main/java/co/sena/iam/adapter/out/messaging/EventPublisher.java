package co.sena.iam.adapter.out.messaging;

/**
 * Sends a serialized domain-event envelope to the broker.
 *
 * <p>Infrastructure seam (not an application port): the use cases already decided what to
 * stage in {@code identity.outbox}; the relay's only job is to get those rows to the broker
 * eventually. Kept as an interface so the relay can be unit-tested without a live broker.
 */
public interface EventPublisher {

    /**
     * @param routingKey the event type (e.g. {@code iam.user.created}); subscribers bind on it
     * @param messageId  the event id, carried as the AMQP message id for broker-level dedup
     * @param body       the full {@code DomainEventEnvelope} JSON (shared-contracts)
     */
    void publish(String routingKey, String messageId, byte[] body);
}
