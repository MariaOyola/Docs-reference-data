package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.iam.application.port.in.AssignRoleCommand;
import co.sena.iam.application.port.in.UserRoleResult;
import co.sena.iam.domain.exception.RoleAlreadyAssignedException;
import co.sena.iam.domain.exception.RoleNotFoundException;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserRoleAssignment;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.InMemoryUserRolePort;
import co.sena.iam.testsupport.RecordingOutboxPort;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssignUserRoleTest {

    private InMemoryUserRepository users;
    private InMemoryUserRolePort rolePort;
    private RecordingOutboxPort outbox;
    private AssignUserRole assignUserRole;

    private UUID userId;
    private UUID adminId;
    private static final UUID COORDINATOR_ROLE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        rolePort = new InMemoryUserRolePort();
        rolePort.seedRole("COORDINATOR", COORDINATOR_ROLE_ID);
        outbox = new RecordingOutboxPort();
        assignUserRole = new AssignUserRole(users, rolePort, outbox);

        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        users.save(new User(userId, "user@sena.edu.co", "hash", "Some", "User",
                ActorType.USER, null, true, LockoutState.clear(), false, null));
    }

    @Test
    @DisplayName("assigns role and stages iam.role.assigned event")
    void assigns_role_and_stages_event() {
        var cmd = new AssignRoleCommand(userId, adminId, "COORDINATOR", null, null);

        UserRoleResult result = assignUserRole.assign(cmd);

        assertThat(result.roleName()).isEqualTo("COORDINATOR");
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(rolePort.allAssignments()).hasSize(1);

        OutboxEvent event = outbox.singleEvent();
        assertThat(event.eventType()).isEqualTo("iam.role.assigned");
        assertThat(event.payload().get("user_id")).isEqualTo(userId.toString());
        assertThat(event.payload().get("role_name")).isEqualTo("COORDINATOR");
    }

    @Test
    @DisplayName("throws RoleNotFoundException when role does not exist")
    void throws_when_role_not_found() {
        var cmd = new AssignRoleCommand(userId, adminId, "NONEXISTENT", null, null);

        assertThatThrownBy(() -> assignUserRole.assign(cmd))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    @DisplayName("throws UserNotFoundException when user does not exist")
    void throws_when_user_not_found() {
        var cmd = new AssignRoleCommand(UUID.randomUUID(), adminId, "COORDINATOR", null, null);

        assertThatThrownBy(() -> assignUserRole.assign(cmd))
                .isInstanceOf(UserNotFoundException.class);
    }
}
