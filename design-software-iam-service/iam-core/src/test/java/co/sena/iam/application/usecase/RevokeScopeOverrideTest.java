package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.iam.domain.exception.ScopeOverrideNotFoundException;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import co.sena.iam.testsupport.InMemoryScopeOverridePort;
import co.sena.iam.testsupport.RecordingOutboxPort;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-006 E3 — DELETE /users/{id}/scope-overrides/{override_id}. */
class RevokeScopeOverrideTest {

    private InMemoryScopeOverridePort overrides;
    private RecordingOutboxPort outbox;
    private RevokeScopeOverride revokeScopeOverride;

    private final UUID userId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        overrides = new InMemoryScopeOverridePort();
        overrides.seedFeature("SCH_CREATE", UUID.randomUUID());
        outbox = new RecordingOutboxPort();
        revokeScopeOverride = new RevokeScopeOverride(overrides, outbox);
    }

    @Test
    @DisplayName("removes the override and stages iam.scope_override.revoked")
    void removesOverrideAndStagesEvent() {
        ScopeOverrideRecord seeded = overrides.grant(
                userId, UUID.randomUUID(), "SCH_CREATE", ScopeType.GLOBAL, true, "x", adminId, null);

        revokeScopeOverride.revoke(userId, seeded.id());

        assertThat(overrides.all()).isEmpty();
        OutboxEvent event = outbox.singleEvent();
        assertThat(event.eventType()).isEqualTo("iam.scope_override.revoked");
        assertThat(event.payload().get("override_id")).isEqualTo(seeded.id().toString());
    }

    @Test
    @DisplayName("rejects an unknown override with ScopeOverrideNotFoundException")
    void rejectsUnknownOverride() {
        assertThatThrownBy(() -> revokeScopeOverride.revoke(userId, UUID.randomUUID()))
                .isInstanceOf(ScopeOverrideNotFoundException.class);
        assertThat(outbox.isEmpty()).isTrue();
    }
}
