package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.ListRolesUseCase;
import co.sena.iam.application.port.out.RbacCatalogRepository;
import co.sena.iam.domain.model.RbacRole;
import java.util.List;
import java.util.Objects;

/** HU-IAM-004 E1 — returns all roles. E2: scope differentiation lives in the features endpoint. */
public final class ListRoles implements ListRolesUseCase {

    private final RbacCatalogRepository catalog;

    public ListRoles(RbacCatalogRepository catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public List<RbacRole> listRoles() {
        return catalog.listRoles();
    }
}
