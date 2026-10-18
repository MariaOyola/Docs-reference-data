package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.RevokeUserRoleUseCase;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.application.port.out.UserRolePort;
import co.sena.iam.domain.exception.RoleNotFoundException;
import co.sena.iam.domain.exception.UserNotFoundException;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-005 E4: revoke a role assignment from a user. */
public final class RevokeUserRole implements RevokeUserRoleUseCase {

    private final UserRepository users;
    private final UserRolePort rolePort;

    public RevokeUserRole(UserRepository users, UserRolePort rolePort) {
        this.users = Objects.requireNonNull(users, "users");
        this.rolePort = Objects.requireNonNull(rolePort, "rolePort");
    }

    @Override
    public void revoke(UUID userId, String roleName, UUID trainingCenterId) {
        users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        boolean removed = rolePort.revoke(userId, roleName, trainingCenterId);
        if (!removed) {
            throw new RoleNotFoundException(roleName);
        }
    }
}
