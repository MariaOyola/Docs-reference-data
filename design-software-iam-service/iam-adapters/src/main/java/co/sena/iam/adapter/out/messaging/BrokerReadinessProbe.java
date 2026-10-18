package co.sena.iam.adapter.out.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Checks broker connectivity for the readiness probe. Only registered when the HU-IAM-007
 * outbox relay's broker ({@code spring.rabbitmq.host}) is configured — see
 * {@code co.sena.iam.api.config.BrokerHealthConfiguration}.
 */
public class BrokerReadinessProbe {

    private final RabbitTemplate rabbitTemplate;

    public BrokerReadinessProbe(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public boolean isUp() {
        try {
            rabbitTemplate.execute(channel -> true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
