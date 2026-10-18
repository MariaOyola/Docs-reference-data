package co.sena.iam.application.port.in;

import java.util.UUID;

/** HU-IAM-006 E3 — DELETE /users/{id}/scope-overrides/{override_id}. */
public interface RevokeScopeOverrideUseCase {

    void revoke(UUID userId, UUID overrideId);
}
