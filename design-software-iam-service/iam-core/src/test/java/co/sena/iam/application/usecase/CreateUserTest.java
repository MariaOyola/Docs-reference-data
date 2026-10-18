package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.iam.application.port.in.CreateUserCommand;
import co.sena.iam.application.port.in.UserCreateResult;
import co.sena.iam.application.port.out.PasswordHasher;
import co.sena.iam.domain.exception.EmailAlreadyExistsException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.User;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.InMemoryUserRolePort;
import co.sena.iam.testsupport.InMemoryUserWritePort;
import co.sena.iam.testsupport.RecordingOutboxPort;
import co.sena.iam.testsupport.StubPasswordGenerator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateUserTest {

    private InMemoryUserRepository users;
    private InMemoryUserWritePort userWriter;
    private InMemoryUserRolePort rolePort;
    private RecordingOutboxPort outbox;
    private CreateUser createUser;

    private static final UUID INSTRUCTOR_ROLE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        userWriter = new InMemoryUserWritePort();
        rolePort = new InMemoryUserRolePort();
        rolePort.seedRole("INSTRUCTOR", INSTRUCTOR_ROLE_ID);
        outbox = new RecordingOutboxPort();
        PasswordHasher stubHasher = new PasswordHasher() {
            @Override public String hash(String raw) { return "hashed:" + raw; }
            @Override public boolean matches(String raw, String hash) { return hash.equals("hashed:" + raw); }
        };
        createUser = new CreateUser(users, userWriter, rolePort, stubHasher, new StubPasswordGenerator(), outbox);
    }

    @Test
    @DisplayName("creates user, saves it, and stages iam.user.created event")
    void creates_user_and_stages_created_event() {
        var cmd = new CreateUserCommand("ana@sena.edu.co", "Ana", "Ríos", ActorType.INSTRUCTOR, null,
                "CC", "1234567890", java.math.BigDecimal.valueOf(40.0), null, null);

        UserCreateResult result = createUser.create(cmd);

        assertThat(result.email()).isEqualTo("ana@sena.edu.co");
        assertThat(result.temporaryPassword()).isEqualTo(StubPasswordGenerator.GENERATED);
        assertThat(result.id()).isNotNull();

        User saved = userWriter.lastSaved();
        assertThat(saved.email()).isEqualTo("ana@sena.edu.co");
        assertThat(saved.active()).isTrue();
        assertThat(saved.firstName()).isEqualTo("Ana");
        assertThat(saved.passwordHash()).isEqualTo("hashed:" + StubPasswordGenerator.GENERATED);

        OutboxEvent event = outbox.singleEvent();
        assertThat(event.eventType()).isEqualTo("iam.user.created");
        assertThat(event.payload()).containsKey("user_id");
        assertThat(event.payload().get("email")).isEqualTo("ana@sena.edu.co");
        assertThat(event.payload().get("first_name")).isEqualTo("Ana");
        assertThat(event.payload().get("last_name")).isEqualTo("Ríos");
        assertThat(event.payload().get("actor_type")).isEqualTo("INSTRUCTOR");
        assertThat(event.payload().get("document_type")).isEqualTo("CC");
        assertThat(event.payload().get("document_number")).isEqualTo("1234567890");
        assertThat(event.payload().get("default_max_hours_per_week")).isEqualTo(java.math.BigDecimal.valueOf(40.0));
        assertThat(event.payload().get("roles")).asList().isEmpty();
    }

    @Test
    @DisplayName("with initial role — assigns role and includes it in the event payload")
    void creates_user_with_initial_role() {
        var cmd = new CreateUserCommand("juan@sena.edu.co", "Juan", "Pérez", ActorType.USER, null, "INSTRUCTOR", null);

        createUser.create(cmd);

        assertThat(rolePort.allAssignments()).hasSize(1);
        assertThat(rolePort.allAssignments().get(0).roleName()).isEqualTo("INSTRUCTOR");

        OutboxEvent event = outbox.singleEvent();
        assertThat(event.payload().get("roles")).asList()
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("role_name", "INSTRUCTOR")
                .containsEntry("training_center_id", null);
    }

    @Test
    @DisplayName("rejects duplicate email")
    void rejects_duplicate_email() {
        UUID existing = UUID.randomUUID();
        users.save(new User(existing, "dup@sena.edu.co", "x", "A", "B", ActorType.USER, null, true, LockoutState.clear(), false, null));

        var cmd = new CreateUserCommand("dup@sena.edu.co", "Dup", "User", ActorType.USER, null, null, null);

        assertThatThrownBy(() -> createUser.create(cmd))
                .isInstanceOf(EmailAlreadyExistsException.class);

        assertThat(outbox.isEmpty()).isTrue();
    }
}
