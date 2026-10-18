package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.OutboxEvent;
import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.User;
import co.sena.iam.testsupport.InMemoryRefreshTokenRepository;
import co.sena.iam.testsupport.InMemoryUserRepository;
import co.sena.iam.testsupport.InMemoryUserWritePort;
import co.sena.iam.testsupport.RecordingOutboxPort;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeactivateUserTest {

    private InMemoryUserRepository users;
    private InMemoryUserWritePort userWriter;
    private InMemoryRefreshTokenRepository refreshTokens;
    private RecordingOutboxPort outbox;
    private DeactivateUser deactivateUser;

    private UUID userId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        userWriter = new InMemoryUserWritePort();
        refreshTokens = new InMemoryRefreshTokenRepository();
        outbox = new RecordingOutboxPort();
        deactivateUser = new DeactivateUser(users, userWriter, refreshTokens, outbox);

        userId = UUID.randomUUID();
        User user = new User(userId, "active@sena.edu.co", "hash", "Active", "User",
                ActorType.INSTRUCTOR, null, true, LockoutState.clear(), false, null);
        users.save(user);
        userWriter.save(user);
    }

    @Test
    @DisplayName("deactivates user, revokes all sessions, and stages iam.user.deactivated event")
    void deactivates_and_stages_event() {
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        Instant now = Instant.now();
        refreshTokens.save(RefreshTokenRecord.newSession(sessionA, userId, "hash-a", null, null, now.plusSeconds(3600), now));
        refreshTokens.save(RefreshTokenRecord.newSession(sessionB, userId, "hash-b", null, null, now.plusSeconds(3600), now));
        assertThat(refreshTokens.activeCount(userId)).isEqualTo(2);

        deactivateUser.deactivate(userId);

        assertThat(userWriter.isDeactivated(userId)).isTrue();
        assertThat(refreshTokens.activeCount(userId)).isZero();

        OutboxEvent event = outbox.singleEvent();
        assertThat(event.eventType()).isEqualTo("iam.user.deactivated");
        assertThat(event.payload().get("user_id")).isEqualTo(userId.toString());
        assertThat(event.payload().get("email")).isEqualTo("active@sena.edu.co");
        assertThat(event.payload()).containsKey("deactivated_at");
    }

    @Test
    @DisplayName("throws UserNotFoundException when user does not exist")
    void throws_when_user_not_found() {
        assertThatThrownBy(() -> deactivateUser.deactivate(UUID.randomUUID()))
                .isInstanceOf(UserNotFoundException.class);
    }
}
