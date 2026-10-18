package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.ActorType;
import java.util.UUID;

public record UpdateUserCommand(
        String firstName,
        String lastName,
        ActorType actorType,
        UUID actorId,
        Boolean isActive) {
}
