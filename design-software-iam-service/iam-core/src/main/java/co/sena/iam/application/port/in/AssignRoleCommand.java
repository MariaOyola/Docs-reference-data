package co.sena.iam.application.port.in;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record AssignRoleCommand(
        UUID userId,
        UUID assignedBy,
        String roleName,
        UUID trainingCenterId,
        OffsetDateTime expiresAt) {

    public AssignRoleCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(assignedBy, "assignedBy");
        Objects.requireNonNull(roleName, "roleName");
    }
}
