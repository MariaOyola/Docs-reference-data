package co.sena.iam.domain.exception;

import java.time.Instant;

/** The account is serving a lockout period (RN-IAM-01). */
public class AccountLockedException extends RuntimeException {

    private final transient Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Account locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}
