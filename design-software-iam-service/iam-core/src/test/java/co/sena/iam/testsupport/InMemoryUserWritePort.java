package co.sena.iam.testsupport;

import co.sena.iam.application.port.in.UpdateUserCommand;
import co.sena.iam.application.port.out.UserWritePort;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InMemoryUserWritePort implements UserWritePort {

    private final List<User> saved = new ArrayList<>();

    @Override
    public void save(User user) {
        saved.add(user);
    }

    @Override
    public User update(UUID id, UpdateUserCommand cmd) {
        User existing = saved.stream().filter(u -> u.id().equals(id)).findFirst()
                .orElseThrow(() -> new UserNotFoundException(id));
        User updated = new User(
                existing.id(),
                existing.email(),
                existing.passwordHash(),
                cmd.firstName() != null ? cmd.firstName() : existing.firstName(),
                cmd.lastName() != null ? cmd.lastName() : existing.lastName(),
                cmd.actorType() != null ? cmd.actorType() : existing.actorType(),
                cmd.actorId() != null ? cmd.actorId() : existing.actorId(),
                cmd.isActive() != null ? cmd.isActive() : existing.active(),
                existing.lockout(),
                existing.mustChangePassword(),
                existing.temporaryPasswordExpiresAt());
        saved.removeIf(u -> u.id().equals(id));
        saved.add(updated);
        return updated;
    }

    @Override
    public void deactivate(UUID id) {
        User existing = saved.stream().filter(u -> u.id().equals(id)).findFirst()
                .orElseThrow(() -> new UserNotFoundException(id));
        User deactivated = new User(
                existing.id(), existing.email(), existing.passwordHash(),
                existing.firstName(), existing.lastName(), existing.actorType(),
                existing.actorId(), false, existing.lockout(),
                existing.mustChangePassword(), existing.temporaryPasswordExpiresAt());
        saved.removeIf(u -> u.id().equals(id));
        saved.add(deactivated);
    }

    public User lastSaved() {
        return saved.get(saved.size() - 1);
    }

    public boolean isDeactivated(UUID id) {
        return saved.stream().filter(u -> u.id().equals(id)).findFirst()
                .map(u -> !u.active()).orElse(false);
    }
}
