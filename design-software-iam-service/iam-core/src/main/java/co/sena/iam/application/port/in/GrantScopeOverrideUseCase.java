package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.ScopeOverrideRecord;

/** HU-IAM-006 E3 — POST /users/{id}/scope-overrides. */
public interface GrantScopeOverrideUseCase {

    ScopeOverrideRecord grant(GrantScopeOverrideCommand command);
}
