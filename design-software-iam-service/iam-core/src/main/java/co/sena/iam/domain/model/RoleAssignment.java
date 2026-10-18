package co.sena.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Role granted to a user, optionally narrowed to one training center.
 * Mirrors {@code rbac.user_role} joined with {@code rbac.role}.
 *
 * @param trainingCenterId null for a role that is not scoped to a center
 */
public record RoleAssignment(String roleName, UUID trainingCenterId) {

    public RoleAssignment {
        Objects.requireNonNull(roleName, "roleName");
    }
}
