package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Outbound port over {@code identity.user}. Implemented by the persistence adapter. */
public interface UserRepository {

    /** Lookup is case-insensitive, matching the {@code uq_user_email_lower} index. */
    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    /** Clears the failed-attempt counter and the lock, and stamps {@code last_login_at}. */
    void registerSuccessfulLogin(UUID userId, Instant at);

    /** Persists the counter and lock computed by {@code LockoutPolicy}. */
    void applyLockoutState(UUID userId, LockoutState state);

    /**
     * Replaces the password hash and clears {@code must_change_password} +
     * {@code password_temporary_expires_at} in one atomic write.
     */
    void updatePassword(UUID userId, String newPasswordHash, Instant updatedAt);
}
