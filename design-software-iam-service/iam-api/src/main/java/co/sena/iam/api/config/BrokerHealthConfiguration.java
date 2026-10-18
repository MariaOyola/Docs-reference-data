package co.sena.iam.api.config;

import co.sena.iam.adapter.out.messaging.BrokerReadinessProbe;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the broker readiness probe only when the HU-IAM-007 outbox relay's broker is
 * actually configured — same gate as {@link MessagingConfiguration}. Without this gate,
 * {@code /ready} would depend on RabbitMQ's default localhost connection even in
 * environments where the relay is intentionally off.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.rabbitmq.host")
class BrokerHealthConfiguration {

    @Bean
    BrokerReadinessProbe brokerReadinessProbe(RabbitTemplate rabbitTemplate) {
        return new BrokerReadinessProbe(rabbitTemplate);
    }
}
