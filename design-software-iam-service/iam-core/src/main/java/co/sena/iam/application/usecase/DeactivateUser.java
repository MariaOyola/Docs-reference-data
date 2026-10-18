package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.DeactivateUserUseCase;
import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.application.port.out.UserWritePort;
import co.sena.iam.domain.exception.UserNotFoundException;
import co.sena.iam.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-005 E3: soft-delete a user and revoke all active sessions. */
public final class DeactivateUser implements DeactivateUserUseCase {

    private final UserRepository users;
    private final UserWritePort userWriter;
    private final RefreshTokenRepository refreshTokens;
    private final OutboxPort outbox;

    public DeactivateUser(
            UserRepository users,
            UserWritePort userWriter,
            RefreshTokenRepository refreshTokens,
            OutboxPort outbox) {
        this.users = Objects.requireNonNull(users, "users");
        this.userWriter = Objects.requireNonNull(userWriter, "userWriter");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.outbox = Objects.requireNonNull(outbox, "outbox");
    }

    @Override
    public void deactivate(UUID userId) {
        var user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        userWriter.deactivate(userId);
        Instant deactivatedAt = Instant.now();
        refreshTokens.revokeAllActiveForUser(userId, deactivatedAt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId.toString());
        payload.put("email", user.email());
        payload.put("deactivated_at", deactivatedAt.toString());
        outbox.stage(OutboxEvent.of("iam.user.deactivated", payload));
    }
}
