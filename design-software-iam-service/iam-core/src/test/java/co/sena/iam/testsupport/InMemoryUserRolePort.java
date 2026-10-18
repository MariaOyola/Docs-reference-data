package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.UserRolePort;
import co.sena.iam.domain.model.UserRoleAssignment;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryUserRolePort implements UserRolePort {

    private final Map<String, UUID> roleIds = new HashMap<>();
    private final List<UserRoleAssignment> assignments = new ArrayList<>();

    public void seedRole(String name, UUID id) {
        roleIds.put(name, id);
    }

    @Override
    public Optional<UUID> findRoleIdByName(String roleName) {
        return Optional.ofNullable(roleIds.get(roleName));
    }

    @Override
    public UserRoleAssignment assign(
            UUID userId,
            UUID roleId,
            String roleName,
            UUID trainingCenterId,
            UUID assignedBy,
            OffsetDateTime expiresAt) {
        UserRoleAssignment a = new UserRoleAssignment(
                UUID.randomUUID(), userId, roleId, roleName,
                trainingCenterId, assignedBy, OffsetDateTime.now(ZoneOffset.UTC), expiresAt);
        assignments.add(a);
        return a;
    }

    @Override
    public boolean revoke(UUID userId, String roleName, UUID trainingCenterId) {
        return assignments.removeIf(a ->
                a.userId().equals(userId)
                        && a.roleName().equals(roleName)
                        && java.util.Objects.equals(a.trainingCenterId(), trainingCenterId));
    }

    public List<UserRoleAssignment> allAssignments() {
        return List.copyOf(assignments);
    }
}
