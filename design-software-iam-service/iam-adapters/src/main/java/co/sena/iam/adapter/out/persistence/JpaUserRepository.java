package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.User;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbound persistence adapter for {@code identity.user}.
 *
 * <p>Each write runs in its own transaction on purpose: a failed login must leave its
 * counter increment behind even though the use case then throws.
 */
@Repository
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository users;

    JpaUserRepository(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return users.findByEmailIgnoringCase(email).map(JpaUserRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return users.findById(id).map(JpaUserRepository::toDomain);
    }

    @Override
    @Transactional
    public void registerSuccessfulLogin(UUID userId, Instant at) {
        users.clearLockoutAndStampLogin(userId, at.atOffset(ZoneOffset.UTC));
    }

    @Override
    @Transactional
    public void updatePassword(UUID userId, String newPasswordHash, Instant updatedAt) {
        users.updatePasswordAndClearMustChange(userId, newPasswordHash, updatedAt.atOffset(ZoneOffset.UTC));
    }

    @Override
    @Transactional
    public void applyLockoutState(UUID userId, LockoutState state) {
        OffsetDateTime lockedUntil = state.lockedUntil() == null
                ? null
                : state.lockedUntil().atOffset(ZoneOffset.UTC);
        users.updateLockout(
                userId,
                (short) state.failedAttempts(),
                lockedUntil,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static User toDomain(UserEntity entity) {
        Instant lockedUntil = entity.getLockedUntil() == null ? null : entity.getLockedUntil().toInstant();
        Instant tempExpires = entity.getTemporaryPasswordExpiresAt() == null
                ? null : entity.getTemporaryPasswordExpiresAt().toInstant();
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFirstName(),
                entity.getLastName(),
                ActorType.valueOf(entity.getActorType()),
                entity.getActorId(),
                entity.isActive(),
                new LockoutState(entity.getFailedAttempts(), lockedUntil),
                entity.isMustChangePassword(),
                tempExpires);
    }
}
