package co.sena.iam.application.port.in;

import java.util.UUID;

public record ListUsersQuery(
        int page,
        int size,
        UUID trainingCenterId,
        String role,
        Boolean isActive) {
}
