package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.AuthenticatedIdentity;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.application.port.out.UserAccessRepository;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.exception.UnauthenticatedException;
import co.sena.iam.domain.model.User;
import java.util.Objects;
import java.util.UUID;

/** HU-IAM-001 E4 — identity, roles and features behind a valid access token. */
public final class GetCurrentUser implements GetCurrentUserUseCase {

    private final AccessTokenVerifier accessTokenVerifier;
    private final UserRepository users;
    private final UserAccessRepository userAccess;

    public GetCurrentUser(
            AccessTokenVerifier accessTokenVerifier,
            UserRepository users,
            UserAccessRepository userAccess) {
        this.accessTokenVerifier = Objects.requireNonNull(accessTokenVerifier, "accessTokenVerifier");
        this.users = Objects.requireNonNull(users, "users");
        this.userAccess = Objects.requireNonNull(userAccess, "userAccess");
    }

    @Override
    public AuthenticatedIdentity currentUser(String accessToken) {
        UUID userId = accessTokenVerifier.verify(accessToken).userId();
        User user = users.findById(userId)
                .orElseThrow(() -> new UnauthenticatedException("Token subject no longer exists"));
        if (!user.active()) {
            throw new UnauthenticatedException("Account is disabled");
        }
        return new AuthenticatedIdentity(user, userAccess.findAccessFor(userId));
    }
}
