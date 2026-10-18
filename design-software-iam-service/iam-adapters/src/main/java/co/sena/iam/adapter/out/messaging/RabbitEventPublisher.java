package co.sena.iam.adapter.out.messaging;

import java.util.Objects;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Publishes envelopes to the {@code iam-events} topic exchange over RabbitMQ.
 *
 * <p>Follows the same {@code <domain>-events} topic-exchange convention as
 * scheduling-events / monitoring-events (ADR-001). The exchange is declared as a bean so
 * {@code RabbitAdmin} creates it on the broker at startup — idempotent, and robust even if
 * the broker's static definitions were not (re)imported.
 */
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, String exchange) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate");
        this.exchange = Objects.requireNonNull(exchange, "exchange");
    }

    @Override
    public void publish(String routingKey, String messageId, byte[] body) {
        Message message = MessageBuilder.withBody(body)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbitTemplate.send(exchange, routingKey, message);
    }
}
