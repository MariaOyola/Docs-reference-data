package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.RbacModule;
import co.sena.iam.domain.model.RbacRole;
import co.sena.iam.domain.model.RoleFeatureEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only port over rbac_catalog and rbac schemas for catalog queries. */
public interface RbacCatalogRepository {

    /** All active modules with their active features. Ordered by display_order. */
    List<RbacModule> listModules();

    /** All roles ordered by name. */
    List<RbacRole> listRoles();

    /**
     * Features granted by a specific role, empty Optional when the role id does not exist.
     * E2: scope_type distinguishes global vs. training-center-restricted access.
     */
    Optional<List<RoleFeatureEntry>> findRoleFeatures(UUID roleId);
}
