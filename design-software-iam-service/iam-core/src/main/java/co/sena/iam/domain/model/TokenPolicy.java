package co.sena.iam.domain.model;

import java.time.Duration;

/** Token lifetimes fixed by the contract: access 15 min, refresh 7 days, password reset 1 h. */
public final class TokenPolicy {

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    /** HU-IAM-003: password-reset tokens expire after one hour. */
    public static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);
    /** HU-IAM-003: temporary passwords (from user creation) expire after 72 hours. */
    public static final Duration TEMPORARY_PASSWORD_TTL = Duration.ofHours(72);

    private TokenPolicy() {
    }
}
