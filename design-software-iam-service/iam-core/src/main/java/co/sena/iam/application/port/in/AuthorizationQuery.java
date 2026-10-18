package co.sena.iam.application.port.in;

import java.util.Objects;
import java.util.UUID;

/**
 * A request to authorize a single operation.
 *
 * @param trainingCenterId the center the operation touches; {@code null} for an operation
 *                         that is not bound to a center
 */
public record AuthorizationQuery(UUID userId, String featureCode, UUID trainingCenterId) {

    public AuthorizationQuery {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(featureCode, "featureCode");
    }
}
