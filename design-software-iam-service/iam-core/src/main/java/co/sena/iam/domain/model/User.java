package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Account able to authenticate. Pure domain entity: no framework, no ORM.
 * Mirrors {@code identity.user}.
 *
 * @param actorId nullable; set when the account is backed by an instructor/learner actor
 */
public record User(
        UUID id,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        ActorType actorType,
        UUID actorId,
        boolean active,
        LockoutState lockout,
        boolean mustChangePassword,
        Instant temporaryPasswordExpiresAt) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(actorType, "actorType");
        Objects.requireNonNull(lockout, "lockout");
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** True while the account is serving a lockout period (see {@link LockoutPolicy}). */
    public boolean isLockedAt(Instant now) {
        return lockout.isLockedAt(now);
    }

    /** True when the temporary password has passed its 72-hour window. */
    public boolean isTemporaryPasswordExpiredAt(Instant now) {
        return temporaryPasswordExpiresAt != null && now.isAfter(temporaryPasswordExpiresAt);
    }

    public User withLockout(LockoutState newLockout) {
        return new User(id, email, passwordHash, firstName, lastName, actorType, actorId, active,
                newLockout, mustChangePassword, temporaryPasswordExpiresAt);
    }
}
