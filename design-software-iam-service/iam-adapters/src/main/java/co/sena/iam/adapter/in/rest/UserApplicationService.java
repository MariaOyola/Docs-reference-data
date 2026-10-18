package co.sena.iam.adapter.in.rest;

import co.sena.iam.application.port.in.AssignRoleCommand;
import co.sena.iam.application.port.in.AssignUserRoleUseCase;
import co.sena.iam.application.port.in.CreateUserCommand;
import co.sena.iam.application.port.in.CreateUserUseCase;
import co.sena.iam.application.port.in.DeactivateUserUseCase;
import co.sena.iam.application.port.in.RevokeUserRoleUseCase;
import co.sena.iam.application.port.in.UpdateUserCommand;
import co.sena.iam.application.port.in.UpdateUserUseCase;
import co.sena.iam.application.port.in.UserCreateResult;
import co.sena.iam.application.port.in.UserRoleResult;
import co.sena.iam.domain.model.User;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction boundary for user-management write operations.
 *
 * <p>iam-core use cases are Spring-free; this adapter wraps them in a single
 * {@code @Transactional} scope so that both the business state change and the
 * outbox event land in the same DB transaction (atomically).
 */
@Service
@Transactional
public class UserApplicationService {

    private final CreateUserUseCase createUser;
    private final UpdateUserUseCase updateUser;
    private final DeactivateUserUseCase deactivateUser;
    private final AssignUserRoleUseCase assignUserRole;
    private final RevokeUserRoleUseCase revokeUserRole;

    public UserApplicationService(
            CreateUserUseCase createUser,
            UpdateUserUseCase updateUser,
            DeactivateUserUseCase deactivateUser,
            AssignUserRoleUseCase assignUserRole,
            RevokeUserRoleUseCase revokeUserRole) {
        this.createUser = createUser;
        this.updateUser = updateUser;
        this.deactivateUser = deactivateUser;
        this.assignUserRole = assignUserRole;
        this.revokeUserRole = revokeUserRole;
    }

    public UserCreateResult createUser(CreateUserCommand cmd) {
        return createUser.create(cmd);
    }

    public User updateUser(UUID userId, UpdateUserCommand cmd) {
        return updateUser.updateUser(userId, cmd);
    }

    public void deactivateUser(UUID userId) {
        deactivateUser.deactivate(userId);
    }

    public UserRoleResult assignRole(AssignRoleCommand cmd) {
        return assignUserRole.assign(cmd);
    }

    public void revokeRole(UUID userId, String roleName, UUID trainingCenterId) {
        revokeUserRole.revoke(userId, roleName, trainingCenterId);
    }
}
