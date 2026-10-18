package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.UserAccess;
import java.util.UUID;

/** Outbound port over the RBAC read model ({@code rbac.*} + {@code rbac_catalog.*}). */
public interface UserAccessRepository {

    /** Never null: a user with no active role assignment gets {@link UserAccess#empty()}. */
    UserAccess findAccessFor(UUID userId);
}
