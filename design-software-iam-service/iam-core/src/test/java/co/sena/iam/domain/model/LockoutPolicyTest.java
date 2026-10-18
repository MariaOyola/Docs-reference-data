package co.sena.iam.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RN-IAM-01 as a pure rule, independent of any port. */
class LockoutPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");

    @Test
    @DisplayName("below the first threshold the counter grows without arming a lock")
    void countsWithoutLockingBelowFirstThreshold() {
        LockoutState state = LockoutState.clear();
        for (int i = 1; i < LockoutPolicy.SHORT_LOCK_THRESHOLD; i++) {
            state = LockoutPolicy.afterFailedAttempt(state, NOW);
            assertEquals(i, state.failedAttempts());
            assertNull(state.lockedUntil(), "no lock before attempt " + LockoutPolicy.SHORT_LOCK_THRESHOLD);
        }
    }

    @Test
    @DisplayName("the 5th consecutive failure locks the account for 15 minutes")
    void locksFifteenMinutesAtFifthFailure() {
        LockoutState state = new LockoutState(4, null);

        LockoutState locked = LockoutPolicy.afterFailedAttempt(state, NOW);

        assertEquals(5, locked.failedAttempts());
        assertEquals(NOW.plus(LockoutPolicy.SHORT_LOCK), locked.lockedUntil());
        assertTrue(locked.isLockedAt(NOW));
    }

    @Test
    @DisplayName("the 10th consecutive failure escalates the lock to 24 hours")
    void locksTwentyFourHoursAtTenthFailure() {
        LockoutState state = new LockoutState(9, null);

        LockoutState locked = LockoutPolicy.afterFailedAttempt(state, NOW);

        assertEquals(10, locked.failedAttempts());
        assertEquals(NOW.plus(LockoutPolicy.LONG_LOCK), locked.lockedUntil());
    }

    @Test
    @DisplayName("a lock that has already elapsed is not a lock")
    void expiredLockIsNotLocked() {
        LockoutState state = new LockoutState(5, NOW.minusSeconds(1));

        assertFalse(state.isLockedAt(NOW));
    }

    @Test
    @DisplayName("a successful login drops both the counter and the lock")
    void successClearsCounterAndLock() {
        LockoutState cleared = LockoutPolicy.afterSuccessfulLogin();

        assertEquals(0, cleared.failedAttempts());
        assertNull(cleared.lockedUntil());
        assertFalse(cleared.isLockedAt(NOW));
    }
}
