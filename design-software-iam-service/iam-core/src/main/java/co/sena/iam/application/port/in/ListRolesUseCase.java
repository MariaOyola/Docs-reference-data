package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.RbacRole;
import java.util.List;

/** HU-IAM-004 E1 — GET /roles. */
public interface ListRolesUseCase {

    List<RbacRole> listRoles();
}
