package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.ActorType;
import java.util.Objects;
import java.util.UUID;

public record CreateUserCommand(
        String email,
        String firstName,
        String lastName,
        ActorType actorType,
        UUID actorId,
        String documentType,
        String documentNumber,
        java.math.BigDecimal defaultMaxHoursPerWeek,
        String initialRole,
        UUID trainingCenterId) {

    public CreateUserCommand(String email, String firstName, String lastName, ActorType actorType,
            UUID actorId, String initialRole, UUID trainingCenterId) {
        this(email, firstName, lastName, actorType, actorId, null, null, null, initialRole, trainingCenterId);
    }

    public CreateUserCommand {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(actorType, "actorType");
    }
}
