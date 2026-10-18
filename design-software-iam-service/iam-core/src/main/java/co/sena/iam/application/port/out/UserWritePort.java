package co.sena.iam.application.port.out;

import co.sena.iam.application.port.in.UpdateUserCommand;
import co.sena.iam.domain.model.User;
import java.util.UUID;

/** Outbound port for mutating {@code identity.user} rows. */
public interface UserWritePort {

    void save(User user);

    User update(UUID id, UpdateUserCommand cmd);

    void deactivate(UUID id);
}
