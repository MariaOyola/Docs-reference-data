package co.sena.iam.adapter.out.messaging;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Drives {@link OutboxRelay#pollAndPublish()} on a fixed interval.
 *
 * <p>Kept separate from the relay so the scheduled call crosses the transactional proxy
 * (a self-invocation would bypass {@code @Transactional}). A broker or DB blip is logged,
 * not rethrown, so a transient fault never kills the scheduler thread — the next tick
 * simply retries the still-unpublished rows.
 */
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final OutboxRelay relay;

    public OutboxRelayScheduler(OutboxRelay relay) {
        this.relay = Objects.requireNonNull(relay, "relay");
    }

    @Scheduled(
            fixedDelayString = "${iam.outbox.relay.poll-interval-ms:5000}",
            initialDelayString = "${iam.outbox.relay.initial-delay-ms:5000}")
    public void poll() {
        try {
            int published = relay.pollAndPublish();
            if (published > 0) {
                log.debug("outbox relay published {} event(s) to iam-events", published);
            }
        } catch (RuntimeException e) {
            log.warn("outbox relay poll failed; will retry on the next tick", e);
        }
    }
}
