package co.sena.iam.adapter.out.ratelimit;

import co.sena.iam.application.port.out.RateLimiter;
import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Sliding-window in-memory rate limiter: max 5 attempts per key within a 10-minute window.
 *
 * <p>Sufficient for password-reset abuse prevention in a single-instance deployment.
 * Replace with a Redis-backed adapter when horizontal scaling is required.
 */
@Component
class InMemoryRateLimiter implements RateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 600L;

    private final ConcurrentHashMap<String, Deque<Instant>> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        Instant windowStart = now.minusSeconds(WINDOW_SECONDS);

        Deque<Instant> hits = windows.computeIfAbsent(key, k -> new LinkedList<>());

        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(windowStart)) {
                hits.pollFirst();
            }
            if (hits.size() >= MAX_ATTEMPTS) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }
}
