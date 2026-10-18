package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.RequestPasswordResetUseCase;
import co.sena.iam.application.port.out.PasswordResetRepository;
import co.sena.iam.application.port.out.RateLimiter;
import co.sena.iam.application.port.out.RefreshTokenGenerator;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.exception.RateLimitExceededException;
import co.sena.iam.domain.model.PasswordResetRecord;
import co.sena.iam.domain.model.RefreshTokenSecret;
import co.sena.iam.domain.model.TokenPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * HU-IAM-003 E3 — request side.
 *
 * <p>Always returns without error even when the email is unknown (prevents user enumeration).
 * Rate-limited per email to stop bulk-reset abuse.
 */
public final class RequestPasswordReset implements RequestPasswordResetUseCase {

    private final UserRepository users;
    private final PasswordResetRepository resetRequests;
    private final RefreshTokenGenerator tokenGenerator;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public RequestPasswordReset(
            UserRepository users,
            PasswordResetRepository resetRequests,
            RefreshTokenGenerator tokenGenerator,
            RateLimiter rateLimiter,
            Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.resetRequests = Objects.requireNonNull(resetRequests, "resetRequests");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void request(String email, String ipAddress) {
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);

        if (!rateLimiter.tryAcquire(normalized)) {
            throw new RateLimitExceededException();
        }

        Optional<co.sena.iam.domain.model.User> found = users.findByEmail(normalized);
        if (found.isEmpty()) {
            return;
        }

        Instant now = clock.instant();
        RefreshTokenSecret secret = tokenGenerator.generate();
        resetRequests.save(PasswordResetRecord.of(
                found.get().id(),
                secret.hash(),
                now.plus(TokenPolicy.PASSWORD_RESET_TTL),
                now,
                ipAddress));
    }
}
