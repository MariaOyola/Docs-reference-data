package co.sena.iam.application.port.in;

import java.util.UUID;

/** E4: revokes a role previously assigned to a user. */
public interface RevokeUserRoleUseCase {

    void revoke(UUID userId, String roleName, UUID trainingCenterId);
}
