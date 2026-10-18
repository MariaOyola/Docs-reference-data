package co.sena.iam.adapter.in.rest;

import co.sena.iam.application.port.in.GrantScopeOverrideCommand;
import co.sena.iam.application.port.in.GrantScopeOverrideUseCase;
import co.sena.iam.application.port.in.ListScopeOverridesUseCase;
import co.sena.iam.application.port.in.RevokeScopeOverrideUseCase;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-IAM-006 E3 — transaction boundary for scope-override operations.
 *
 * <p>Wraps the Spring-free iam-core use cases so that a write and the outbox event it stages
 * land in the same DB transaction.
 */
@Service
@Transactional
public class ScopeOverrideApplicationService {

    private final GrantScopeOverrideUseCase grantScopeOverride;
    private final RevokeScopeOverrideUseCase revokeScopeOverride;
    private final ListScopeOverridesUseCase listScopeOverrides;

    public ScopeOverrideApplicationService(
            GrantScopeOverrideUseCase grantScopeOverride,
            RevokeScopeOverrideUseCase revokeScopeOverride,
            ListScopeOverridesUseCase listScopeOverrides) {
        this.grantScopeOverride = grantScopeOverride;
        this.revokeScopeOverride = revokeScopeOverride;
        this.listScopeOverrides = listScopeOverrides;
    }

    public ScopeOverrideRecord grant(GrantScopeOverrideCommand command) {
        return grantScopeOverride.grant(command);
    }

    public void revoke(UUID userId, UUID overrideId) {
        revokeScopeOverride.revoke(userId, overrideId);
    }

    @Transactional(readOnly = true)
    public List<ScopeOverrideRecord> list(UUID userId) {
        return listScopeOverrides.list(userId);
    }
}
