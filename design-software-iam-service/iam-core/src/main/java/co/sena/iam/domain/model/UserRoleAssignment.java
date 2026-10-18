package co.sena.iam.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * One row of {@code rbac.user_role} enriched with the role name for use in responses.
 *
 * @param trainingCenterId null for a global assignment
 * @param expiresAt        null for a permanent assignment
 */
public record UserRoleAssignment(
        UUID id,
        UUID userId,
        UUID roleId,
        String roleName,
        UUID trainingCenterId,
        UUID assignedBy,
        OffsetDateTime assignedAt,
        OffsetDateTime expiresAt) {

    public UserRoleAssignment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(roleName, "roleName");
        Objects.requireNonNull(assignedBy, "assignedBy");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
