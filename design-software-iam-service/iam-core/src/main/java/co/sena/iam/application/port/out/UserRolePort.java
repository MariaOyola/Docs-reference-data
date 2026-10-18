package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.UserRoleAssignment;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Outbound port over {@code rbac.user_role} and the supporting {@code rbac.role} catalog. */
public interface UserRolePort {

    Optional<UUID> findRoleIdByName(String roleName);

    UserRoleAssignment assign(
            UUID userId,
            UUID roleId,
            String roleName,
            UUID trainingCenterId,
            UUID assignedBy,
            OffsetDateTime expiresAt);

    /** Revokes the matching (userId, roleName, trainingCenterId) row. Returns false if not found. */
    boolean revoke(UUID userId, String roleName, UUID trainingCenterId);
}
