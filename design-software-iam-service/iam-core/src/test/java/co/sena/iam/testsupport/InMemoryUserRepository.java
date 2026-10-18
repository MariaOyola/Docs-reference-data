package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.model.LockoutPolicy;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.User;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory stand-in for {@code identity.user}, mirroring the adapter's write semantics. */
public final class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> byId = new HashMap<>();
    private Instant lastLoginAt;

    public void save(User user) {
        byId.put(user.id(), user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String needle = email.toLowerCase(Locale.ROOT);
        return byId.values().stream()
                .filter(u -> u.email().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void registerSuccessfulLogin(UUID userId, Instant at) {
        lastLoginAt = at;
        byId.computeIfPresent(userId, (id, u) -> u.withLockout(LockoutPolicy.afterSuccessfulLogin()));
    }

    @Override
    public void applyLockoutState(UUID userId, LockoutState state) {
        byId.computeIfPresent(userId, (id, u) -> u.withLockout(state));
    }

    @Override
    public void updatePassword(UUID userId, String newPasswordHash, Instant updatedAt) {
        byId.computeIfPresent(userId, (id, u) -> new User(
                u.id(), u.email(), newPasswordHash, u.firstName(), u.lastName(),
                u.actorType(), u.actorId(), u.active(), u.lockout(), false, null));
    }

    public LockoutState lockoutOf(UUID userId) {
        return byId.get(userId).lockout();
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }
}
