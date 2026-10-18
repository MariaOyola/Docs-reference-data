package co.sena.iam.application.port.in;

/** Inbound port — HU-IAM-001 E4: resolve the caller of {@code GET /auth/me}. */
public interface GetCurrentUserUseCase {

    /**
     * @param accessToken raw bearer token taken from the Authorization header
     * @throws co.sena.iam.domain.exception.UnauthenticatedException token invalid, expired,
     *                                                              or its subject is gone
     */
    AuthenticatedIdentity currentUser(String accessToken);
}
