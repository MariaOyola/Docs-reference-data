package co.sena.iam.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * What a user may reach: the roles granted to them and the RBAC feature/module codes
 * those roles resolve to.
 */
public record UserAccess(
        List<RoleAssignment> roles,
        List<String> featureCodes,
        List<String> moduleCodes) {

    public UserAccess {
        roles = List.copyOf(Objects.requireNonNull(roles, "roles"));
        featureCodes = List.copyOf(Objects.requireNonNull(featureCodes, "featureCodes"));
        moduleCodes = List.copyOf(Objects.requireNonNull(moduleCodes, "moduleCodes"));
    }

    public static UserAccess empty() {
        return new UserAccess(List.of(), List.of(), List.of());
    }

    /** Role names only — what the access token and {@code TokenResponse.user.roles} carry. */
    public List<String> roleNames() {
        return roles.stream().map(RoleAssignment::roleName).distinct().toList();
    }

    /** The first training center a role is scoped to, or null when none is. */
    public java.util.UUID primaryTrainingCenterId() {
        return roles.stream()
                .map(RoleAssignment::trainingCenterId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
