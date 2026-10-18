package co.sena.iam.application.port.in;

/** HU-IAM-002 E1 — issue a new access token using a still-valid refresh token. */
public interface RefreshAccessTokenUseCase {

    /**
     * @param refreshTokenValue opaque value sent by the client
     * @return new access token; the refresh token itself is unchanged
     * @throws co.sena.iam.domain.exception.InvalidRefreshTokenException token unknown, expired or revoked
     */
    AuthenticationResult refresh(String refreshTokenValue);
}
