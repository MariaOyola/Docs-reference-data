package co.sena.iam.domain.exception;

/** Too many password-reset requests from the same origin. Maps to 429 IAM_RATE_LIMIT_EXCEEDED. */
public final class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Rate limit exceeded for password-reset requests");
    }
}
