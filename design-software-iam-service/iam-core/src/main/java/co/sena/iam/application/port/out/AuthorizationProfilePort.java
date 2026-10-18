package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.AuthorizationProfile;
import java.util.UUID;

/**
 * Reads the authorization read model for a user: their active role-derived feature grants
 * ({@code rbac.user_role} + {@code rbac.role_feature}) and their scope overrides
 * ({@code rbac.user_scope_override}).
 */
public interface AuthorizationProfilePort {

    /** Never null: an unknown user resolves to {@link AuthorizationProfile#empty()}. */
    AuthorizationProfile loadProfile(UUID userId);
}
