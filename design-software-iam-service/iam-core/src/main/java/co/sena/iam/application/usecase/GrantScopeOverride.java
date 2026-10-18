package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.GrantScopeOverrideCommand;
import co.sena.iam.application.port.in.GrantScopeOverrideUseCase;
import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.application.port.out.ScopeOverridePort;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.exception.FeatureNotFoundException;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-006 E3 — grants or blocks a feature for one user, exceptionally. */
public final class GrantScopeOverride implements GrantScopeOverrideUseCase {

    private final UserRepository users;
    private final ScopeOverridePort overrides;
    private final OutboxPort outbox;

    public GrantScopeOverride(UserRepository users, ScopeOverridePort overrides, OutboxPort outbox) {
        this.users = Objects.requireNonNull(users, "users");
        this.overrides = Objects.requireNonNull(overrides, "overrides");
        this.outbox = Objects.requireNonNull(outbox, "outbox");
    }

    @Override
    public ScopeOverrideRecord grant(GrantScopeOverrideCommand command) {
        Objects.requireNonNull(command, "command");

        users.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        UUID featureId = overrides.findFeatureIdByCode(command.featureCode())
                .orElseThrow(() -> new FeatureNotFoundException(command.featureCode()));

        ScopeOverrideRecord created = overrides.grant(
                command.userId(),
                featureId,
                command.featureCode(),
                command.scopeType(),
                command.allowed(),
                command.reason(),
                command.grantedBy(),
                command.expiresAt());

        Map<String, Object> payload = new HashMap<>();
        payload.put("override_id", created.id().toString());
        payload.put("user_id", command.userId().toString());
        payload.put("feature_code", command.featureCode());
        payload.put("scope_type", command.scopeType().name());
        payload.put("is_allowed", command.allowed());
        outbox.stage(OutboxEvent.of("iam.scope_override.granted", payload));

        return created;
    }
}
