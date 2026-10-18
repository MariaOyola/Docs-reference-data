package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.iam.application.port.in.GrantScopeOverrideCommand;
import co.sena.iam.domain.exception.FeatureNotFoundException;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import co.sena.iam.domain.model.User;
import co.sena.iam.testsupport.InMemoryScopeOverridePort;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.RecordingOutboxPort;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-006 E3 — POST /users/{id}/scope-overrides. */
class GrantScopeOverrideTest {

    private InMemoryUserRepository users;
    private InMemoryScopeOverridePort overrides;
    private RecordingOutboxPort outbox;
    private GrantScopeOverride grantScopeOverride;

    private UUID userId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        overrides = new InMemoryScopeOverridePort();
        overrides.seedFeature("SCH_CREATE", UUID.randomUUID());
        outbox = new RecordingOutboxPort();
        grantScopeOverride = new GrantScopeOverride(users, overrides, outbox);

        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        users.save(new User(userId, "user@sena.edu.co", "hash", "Some", "User",
                ActorType.USER, null, true, LockoutState.clear(), false, null));
    }

    @Test
    @DisplayName("creates the override and stages iam.scope_override.granted")
    void createsOverrideAndStagesEvent() {
        var cmd = new GrantScopeOverrideCommand(
                userId, "SCH_CREATE", ScopeType.GLOBAL, true, "temporary coverage", adminId, null);

        ScopeOverrideRecord created = grantScopeOverride.grant(cmd);

        assertThat(created.featureCode()).isEqualTo("SCH_CREATE");
        assertThat(created.allowed()).isTrue();
        assertThat(created.grantedBy()).isEqualTo(adminId);
        assertThat(overrides.all()).hasSize(1);

        OutboxEvent event = outbox.singleEvent();
        assertThat(event.eventType()).isEqualTo("iam.scope_override.granted");
        assertThat(event.payload().get("user_id")).isEqualTo(userId.toString());
        assertThat(event.payload().get("feature_code")).isEqualTo("SCH_CREATE");
    }

    @Test
    @DisplayName("rejects an unknown user with UserNotFoundException")
    void rejectsUnknownUser() {
        var cmd = new GrantScopeOverrideCommand(
                UUID.randomUUID(), "SCH_CREATE", ScopeType.GLOBAL, true, "x", adminId, null);

        assertThatThrownBy(() -> grantScopeOverride.grant(cmd))
                .isInstanceOf(UserNotFoundException.class);
        assertThat(outbox.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("rejects an unknown feature with FeatureNotFoundException")
    void rejectsUnknownFeature() {
        var cmd = new GrantScopeOverrideCommand(
                userId, "DOES_NOT_EXIST", ScopeType.GLOBAL, true, "x", adminId, null);

        assertThatThrownBy(() -> grantScopeOverride.grant(cmd))
                .isInstanceOf(FeatureNotFoundException.class);
        assertThat(overrides.all()).isEmpty();
    }
}
