package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.RoleFeatureEntry;
import java.util.List;
import java.util.UUID;

/** HU-IAM-004 E1/E3 — GET /roles/{id}/features. */
public interface ListRoleFeaturesUseCase {

    /**
     * Returns features for the given role.
     *
     * @throws co.sena.iam.domain.exception.RoleNotFoundException when the role id does not exist
     */
    List<RoleFeatureEntry> listRoleFeatures(UUID roleId);
}
