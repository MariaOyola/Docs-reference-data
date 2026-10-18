package co.sena.iam.api.config;

import co.sena.iam.adapter.out.messaging.EventPublisher;
import co.sena.iam.adapter.out.messaging.OutboxRelay;
import co.sena.iam.adapter.out.messaging.OutboxRelayScheduler;
import co.sena.iam.adapter.out.messaging.RabbitEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the HU-IAM-007 outbox relay: it declares the {@code iam-events} topic exchange,
 * publishes staged events to it, and schedules the poller.
 *
 * <p>Gated on a configured broker so the slices and DB-only integration tests — which run
 * without RabbitMQ — neither declare the exchange nor start the poller. Same idea as
 * {@link PersistenceConfiguration}, which is gated on a datasource.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.rabbitmq.host")
@EnableScheduling
class MessagingConfiguration {

    @Bean
    TopicExchange iamEventsExchange(@Value("${iam.events.exchange:iam-events}") String exchange) {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    @Bean
    EventPublisher eventPublisher(
            RabbitTemplate rabbitTemplate, @Value("${iam.events.exchange:iam-events}") String exchange) {
        return new RabbitEventPublisher(rabbitTemplate, exchange);
    }

    @Bean
    OutboxRelay outboxRelay(
            JdbcTemplate jdbc,
            EventPublisher publisher,
            ObjectMapper mapper,
            @Value("${iam.outbox.relay.batch-size:100}") int batchSize) {
        return new OutboxRelay(jdbc, publisher, mapper, batchSize);
    }

    @Bean
    OutboxRelayScheduler outboxRelayScheduler(OutboxRelay relay) {
        return new OutboxRelayScheduler(relay);
    }
}
