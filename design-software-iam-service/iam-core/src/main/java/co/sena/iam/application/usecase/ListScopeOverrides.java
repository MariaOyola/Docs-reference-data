package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.ListScopeOverridesUseCase;
import co.sena.iam.application.port.out.ScopeOverridePort;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-006 E3 — lists the overrides currently in force for a user. */
public final class ListScopeOverrides implements ListScopeOverridesUseCase {

    private final UserRepository users;
    private final ScopeOverridePort overrides;
    private final Clock clock;

    public ListScopeOverrides(UserRepository users, ScopeOverridePort overrides, Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.overrides = Objects.requireNonNull(overrides, "overrides");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<ScopeOverrideRecord> list(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return overrides.findActiveByUser(userId, clock.instant());
    }
}
