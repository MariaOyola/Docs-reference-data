package co.sena.iam.application.port.out;

/**
 * Sliding-window rate limiter. The adapter decides the window and ceiling;
 * the domain only sees a boolean gate.
 */
public interface RateLimiter {

    /**
     * Returns {@code true} if the action for {@code key} is still within quota,
     * {@code false} (and counts the attempt) if the limit is already exhausted.
     */
    boolean tryAcquire(String key);
}
