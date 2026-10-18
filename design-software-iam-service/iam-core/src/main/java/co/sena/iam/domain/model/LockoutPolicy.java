package co.sena.iam.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * RN-IAM-01 — progressive lockout after consecutive failed logins.
 *
 * <ul>
 *   <li>5 consecutive failures lock the account for 15 minutes.</li>
 *   <li>10 consecutive failures lock it for 24 hours.</li>
 *   <li>A successful login clears both the counter and the lock.</li>
 * </ul>
 *
 * The counter keeps growing between the two thresholds, so a failure at 6, 7, 8 or 9
 * re-arms the 15-minute lock and the 10th failure escalates to 24 hours.
 */
public final class LockoutPolicy {

    public static final int SHORT_LOCK_THRESHOLD = 5;
    public static final int LONG_LOCK_THRESHOLD = 10;
    public static final Duration SHORT_LOCK = Duration.ofMinutes(15);
    public static final Duration LONG_LOCK = Duration.ofHours(24);

    private LockoutPolicy() {
    }

    /** State to persist after one more failed attempt. */
    public static LockoutState afterFailedAttempt(LockoutState current, Instant now) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(now, "now");
        int attempts = current.failedAttempts() + 1;
        if (attempts >= LONG_LOCK_THRESHOLD) {
            return new LockoutState(attempts, now.plus(LONG_LOCK));
        }
        if (attempts >= SHORT_LOCK_THRESHOLD) {
            return new LockoutState(attempts, now.plus(SHORT_LOCK));
        }
        return new LockoutState(attempts, null);
    }

    /** State to persist after a successful login: counter and lock are dropped. */
    public static LockoutState afterSuccessfulLogin() {
        return LockoutState.clear();
    }
}
