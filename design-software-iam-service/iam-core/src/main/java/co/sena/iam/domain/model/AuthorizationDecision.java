package co.sena.iam.domain.model;

import java.util.Objects;

/** Outcome of evaluating one (user, feature, target center) authorization request. */
public record AuthorizationDecision(boolean permitted, DecisionReason reason) {

    public AuthorizationDecision {
        Objects.requireNonNull(reason, "reason");
    }

    public static AuthorizationDecision permit(DecisionReason reason) {
        return new AuthorizationDecision(true, reason);
    }

    public static AuthorizationDecision deny(DecisionReason reason) {
        return new AuthorizationDecision(false, reason);
    }
}
