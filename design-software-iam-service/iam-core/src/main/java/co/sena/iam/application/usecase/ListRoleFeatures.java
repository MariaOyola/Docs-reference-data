package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.ListRoleFeaturesUseCase;
import co.sena.iam.application.port.out.RbacCatalogRepository;
import co.sena.iam.domain.exception.RoleNotFoundException;
import co.sena.iam.domain.model.RoleFeatureEntry;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU-IAM-004 E1/E3 — returns features granted by a role, each with the scope_type
 * that distinguishes global vs. training-center-restricted access (E2).
 */
public final class ListRoleFeatures implements ListRoleFeaturesUseCase {

    private final RbacCatalogRepository catalog;

    public ListRoleFeatures(RbacCatalogRepository catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public List<RoleFeatureEntry> listRoleFeatures(UUID roleId) {
        Objects.requireNonNull(roleId, "roleId");
        return catalog.findRoleFeatures(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
    }
}
