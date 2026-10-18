package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.RbacCatalogRepository;
import co.sena.iam.domain.model.RbacModule;
import co.sena.iam.domain.model.RbacRole;
import co.sena.iam.domain.model.RoleFeatureEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory stub for RBAC catalog use case tests. */
public final class InMemoryRbacCatalogRepository implements RbacCatalogRepository {

    private final List<RbacModule> modules;
    private final List<RbacRole> roles;
    private final Map<UUID, List<RoleFeatureEntry>> featuresByRole;

    public InMemoryRbacCatalogRepository(
            List<RbacModule> modules,
            List<RbacRole> roles,
            Map<UUID, List<RoleFeatureEntry>> featuresByRole) {
        this.modules = modules == null ? List.of() : List.copyOf(modules);
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.featuresByRole = featuresByRole == null ? Map.of() : Map.copyOf(featuresByRole);
    }

    @Override
    public List<RbacModule> listModules() {
        return modules;
    }

    @Override
    public List<RbacRole> listRoles() {
        return roles;
    }

    @Override
    public Optional<List<RoleFeatureEntry>> findRoleFeatures(UUID roleId) {
        return Optional.ofNullable(featuresByRole.get(roleId));
    }
}
