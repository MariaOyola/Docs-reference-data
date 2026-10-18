package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.AssignRoleCommand;
import co.sena.iam.application.port.in.CreateUserCommand;
import co.sena.iam.application.port.in.CreateUserUseCase;
import co.sena.iam.application.port.in.UserCreateResult;
import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.application.port.out.PasswordGenerator;
import co.sena.iam.application.port.out.PasswordHasher;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.application.port.out.UserRolePort;
import co.sena.iam.application.port.out.UserWritePort;
import co.sena.iam.domain.exception.EmailAlreadyExistsException;
import co.sena.iam.domain.exception.RoleNotFoundException;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-005 E1: create a user account with a temporary password. */
public final class CreateUser implements CreateUserUseCase {

    private final UserRepository users;
    private final UserWritePort userWriter;
    private final UserRolePort rolePort;
    private final PasswordHasher passwordHasher;
    private final PasswordGenerator passwordGenerator;
    private final OutboxPort outbox;

    public CreateUser(
            UserRepository users,
            UserWritePort userWriter,
            UserRolePort rolePort,
            PasswordHasher passwordHasher,
            PasswordGenerator passwordGenerator,
            OutboxPort outbox) {
        this.users = Objects.requireNonNull(users, "users");
        this.userWriter = Objects.requireNonNull(userWriter, "userWriter");
        this.rolePort = Objects.requireNonNull(rolePort, "rolePort");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.passwordGenerator = Objects.requireNonNull(passwordGenerator, "passwordGenerator");
        this.outbox = Objects.requireNonNull(outbox, "outbox");
    }

    @Override
    public UserCreateResult create(CreateUserCommand command) {
        Objects.requireNonNull(command, "command");

        if (users.findByEmail(command.email()).isPresent()) {
            throw new EmailAlreadyExistsException(command.email());
        }

        String tempPassword = passwordGenerator.generate();
        String hash = passwordHasher.hash(tempPassword);
        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                command.email(),
                hash,
                command.firstName(),
                command.lastName(),
                command.actorType(),
                command.actorId(),
                true,
                LockoutState.clear(),
                true,
                null);

        userWriter.save(user);

        if (command.initialRole() != null) {
            UUID roleId = rolePort.findRoleIdByName(command.initialRole())
                    .orElseThrow(() -> new RoleNotFoundException(command.initialRole()));
            rolePort.assign(userId, roleId, command.initialRole(),
                    command.trainingCenterId(), userId, null);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId.toString());
        payload.put("email", command.email());
        payload.put("first_name", command.firstName());
        payload.put("last_name", command.lastName());
        payload.put("actor_type", command.actorType().name());
        if (command.actorId() != null) {
            payload.put("actor_id", command.actorId().toString());
        }
        if (command.documentType() != null) {
            payload.put("document_type", command.documentType());
        }
        if (command.documentNumber() != null) {
            payload.put("document_number", command.documentNumber());
        }
        if (command.defaultMaxHoursPerWeek() != null) {
            payload.put("default_max_hours_per_week", command.defaultMaxHoursPerWeek());
        }
        if (command.initialRole() == null) {
            payload.put("roles", List.of());
        } else {
            Map<String, Object> role = new HashMap<>();
            role.put("role_name", command.initialRole());
            role.put("training_center_id", command.trainingCenterId() == null
                    ? null : command.trainingCenterId().toString());
            payload.put("roles", List.of(role));
        }
        outbox.stage(OutboxEvent.of("iam.user.created", payload));

        return new UserCreateResult(userId, command.email(), tempPassword);
    }
}
