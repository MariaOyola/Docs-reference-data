package co.sena.iam.application.port.in;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record UserRoleResult(
        UUID id,
        UUID userId,
        UUID roleId,
        String roleName,
        UUID trainingCenterId,
        UUID assignedBy,
        OffsetDateTime assignedAt,
        OffsetDateTime expiresAt) {

    public UserRoleResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(roleName, "roleName");
        Objects.requireNonNull(assignedBy, "assignedBy");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
