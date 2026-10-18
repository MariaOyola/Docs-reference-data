package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import co.sena.iam.domain.model.User;
import co.sena.iam.testsupport.InMemoryScopeOverridePort;
import co.sena.iam.testsupport.InMemoryUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-006 E3 — GET /users/{id}/scope-overrides returns active overrides only. */
class ListScopeOverridesTest {

    private InMemoryUserRepository users;
    private InMemoryScopeOverridePort overrides;
    private ListScopeOverrides listScopeOverrides;

    private final Instant now = Instant.parse("2026-08-26T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private UUID userId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        overrides = new InMemoryScopeOverridePort();
        listScopeOverrides = new ListScopeOverrides(users, overrides, clock);

        userId = UUID.randomUUID();
        users.save(new User(userId, "user@sena.edu.co", "hash", "Some", "User",
                ActorType.USER, null, true, LockoutState.clear(), false, null));
    }

    @Test
    @DisplayName("returns active overrides and hides expired ones")
    void returnsActiveOverridesOnly() {
        overrides.grant(userId, UUID.randomUUID(), "SCH_CREATE", ScopeType.GLOBAL, true, "active", userId, null);
        overrides.grant(userId, UUID.randomUUID(), "SCH_EDIT", ScopeType.GLOBAL, true, "expired", userId,
                OffsetDateTime.ofInstant(now.minusSeconds(3600), ZoneOffset.UTC));

        List<ScopeOverrideRecord> result = listScopeOverrides.list(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).featureCode()).isEqualTo("SCH_CREATE");
    }

    @Test
    @DisplayName("rejects an unknown user with UserNotFoundException")
    void rejectsUnknownUser() {
        assertThatThrownBy(() -> listScopeOverrides.list(UUID.randomUUID()))
                .isInstanceOf(UserNotFoundException.class);
    }
}
