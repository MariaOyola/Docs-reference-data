package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.AssignRoleCommand;
import co.sena.iam.application.port.in.AssignUserRoleUseCase;
import co.sena.iam.application.port.in.UserRoleResult;
import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.application.port.out.UserRolePort;
import co.sena.iam.domain.exception.RoleNotFoundException;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.UserRoleAssignment;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-005 E4: assign a role to a user with an optional training-center scope. */
public final class AssignUserRole implements AssignUserRoleUseCase {

    private final UserRepository users;
    private final UserRolePort rolePort;
    private final OutboxPort outbox;

    public AssignUserRole(UserRepository users, UserRolePort rolePort, OutboxPort outbox) {
        this.users = Objects.requireNonNull(users, "users");
        this.rolePort = Objects.requireNonNull(rolePort, "rolePort");
        this.outbox = Objects.requireNonNull(outbox, "outbox");
    }

    @Override
    public UserRoleResult assign(AssignRoleCommand command) {
        Objects.requireNonNull(command, "command");

        users.findById(command.userId()).orElseThrow(() -> new UserNotFoundException(command.userId()));

        UUID roleId = rolePort.findRoleIdByName(command.roleName())
                .orElseThrow(() -> new RoleNotFoundException(command.roleName()));

        UserRoleAssignment assignment = rolePort.assign(
                command.userId(),
                roleId,
                command.roleName(),
                command.trainingCenterId(),
                command.assignedBy(),
                command.expiresAt());

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", command.userId().toString());
        payload.put("role_name", command.roleName());
        if (command.trainingCenterId() != null) {
            payload.put("training_center_id", command.trainingCenterId().toString());
        }
        outbox.stage(OutboxEvent.of("iam.role.assigned", payload));

        return new UserRoleResult(
                assignment.id(),
                assignment.userId(),
                assignment.roleId(),
                assignment.roleName(),
                assignment.trainingCenterId(),
                assignment.assignedBy(),
                assignment.assignedAt(),
                assignment.expiresAt());
    }
}
