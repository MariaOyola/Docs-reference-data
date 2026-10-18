package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.ScopeOverrideRecord;
import java.util.List;
import java.util.UUID;

/** HU-IAM-006 E3 — GET /users/{id}/scope-overrides (active overrides only). */
public interface ListScopeOverridesUseCase {

    List<ScopeOverrideRecord> list(UUID userId);
}
