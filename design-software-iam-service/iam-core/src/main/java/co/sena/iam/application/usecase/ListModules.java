package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.ListModulesUseCase;
import co.sena.iam.application.port.out.RbacCatalogRepository;
import co.sena.iam.domain.model.RbacModule;
import java.util.List;
import java.util.Objects;

/** HU-IAM-004 E1 — returns all active modules with their features. */
public final class ListModules implements ListModulesUseCase {

    private final RbacCatalogRepository catalog;

    public ListModules(RbacCatalogRepository catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public List<RbacModule> listModules() {
        return catalog.listModules();
    }
}
