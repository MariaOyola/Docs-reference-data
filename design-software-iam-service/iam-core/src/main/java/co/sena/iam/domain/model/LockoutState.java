package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Failed-login counter and lock expiry of an account.
 * Mirrors {@code identity.user.failed_attempts} / {@code identity.user.locked_until}.
 *
 * @param failedAttempts consecutive failed attempts; reset to 0 on a successful login
 * @param lockedUntil    instant the lock expires, or {@code null} when not locked
 */
public record LockoutState(int failedAttempts, Instant lockedUntil) {

    public LockoutState {
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("failedAttempts must not be negative");
        }
    }

    /** State of an account with no failed attempts and no lock. */
    public static LockoutState clear() {
        return new LockoutState(0, null);
    }

    /** True while {@code now} is before the lock expiry. An expired lock is not a lock. */
    public boolean isLockedAt(Instant now) {
        Objects.requireNonNull(now, "now");
        return lockedUntil != null && now.isBefore(lockedUntil);
    }
}
