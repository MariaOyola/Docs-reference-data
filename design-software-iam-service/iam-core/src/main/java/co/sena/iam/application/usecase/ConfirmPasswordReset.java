package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.ConfirmPasswordResetUseCase;
import co.sena.iam.application.port.out.PasswordHasher;
import co.sena.iam.application.port.out.PasswordResetRepository;
import co.sena.iam.application.port.out.RefreshTokenGenerator;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.exception.InvalidResetTokenException;
import co.sena.iam.domain.model.PasswordResetRecord;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * HU-IAM-003 E3 — confirm side.
 *
 * <p>Validates the token, replaces the password, clears {@code must_change_password} and
 * revokes all active sessions so the new password is required on every device.
 */
public final class ConfirmPasswordReset implements ConfirmPasswordResetUseCase {

    private final PasswordResetRepository resetRequests;
    private final UserRepository users;
    private final RefreshTokenRepository sessions;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenGenerator tokenGenerator;
    private final Clock clock;

    public ConfirmPasswordReset(
            PasswordResetRepository resetRequests,
            UserRepository users,
            RefreshTokenRepository sessions,
            PasswordHasher passwordHasher,
            RefreshTokenGenerator tokenGenerator,
            Clock clock) {
        this.resetRequests = Objects.requireNonNull(resetRequests, "resetRequests");
        this.users = Objects.requireNonNull(users, "users");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void confirm(String token, String newPassword) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(newPassword, "newPassword");

        String tokenHash = tokenGenerator.hashOf(token);
        Instant now = clock.instant();

        Optional<PasswordResetRecord> found = resetRequests.findActiveByTokenHash(tokenHash);
        if (found.isEmpty()) {
            throw new InvalidResetTokenException();
        }
        PasswordResetRecord record = found.get();

        if (record.used() || record.isExpiredAt(now)) {
            throw new InvalidResetTokenException();
        }

        String newHash = passwordHasher.hash(newPassword);
        users.updatePassword(record.userId(), newHash, now);
        sessions.revokeAllByUserId(record.userId(), now);
        resetRequests.markUsed(record);
    }
}
