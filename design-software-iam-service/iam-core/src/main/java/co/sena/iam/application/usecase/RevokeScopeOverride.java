package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.RevokeScopeOverrideUseCase;
import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.application.port.out.ScopeOverridePort;
import co.sena.iam.domain.exception.ScopeOverrideNotFoundException;
import co.sena.iam.domain.model.OutboxEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-006 E3 — removes an individual scope override. */
public final class RevokeScopeOverride implements RevokeScopeOverrideUseCase {

    private final ScopeOverridePort overrides;
    private final OutboxPort outbox;

    public RevokeScopeOverride(ScopeOverridePort overrides, OutboxPort outbox) {
        this.overrides = Objects.requireNonNull(overrides, "overrides");
        this.outbox = Objects.requireNonNull(outbox, "outbox");
    }

    @Override
    public void revoke(UUID userId, UUID overrideId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(overrideId, "overrideId");

        if (!overrides.revoke(userId, overrideId)) {
            throw new ScopeOverrideNotFoundException(overrideId);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("override_id", overrideId.toString());
        payload.put("user_id", userId.toString());
        outbox.stage(OutboxEvent.of("iam.scope_override.revoked", payload));
    }
}
