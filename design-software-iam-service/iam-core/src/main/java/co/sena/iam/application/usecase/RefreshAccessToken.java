package co.sena.iam.application.usecase;

import co.sena.iam.application.port.in.AuthenticationResult;
import co.sena.iam.application.port.in.RefreshAccessTokenUseCase;
import co.sena.iam.application.port.out.AccessTokenIssuer;
import co.sena.iam.application.port.out.AuthorizationProfilePort;
import co.sena.iam.application.port.out.RefreshTokenGenerator;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.application.port.out.UserAccessRepository;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.domain.exception.InvalidRefreshTokenException;
import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.TokenPolicy;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import co.sena.iam.domain.service.AuthorizationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** HU-IAM-002 E1 — issue a new access token for a valid (non-revoked, non-expired) refresh token. */
public final class RefreshAccessToken implements RefreshAccessTokenUseCase {

    private final UserRepository users;
    private final UserAccessRepository userAccess;
    private final AuthorizationProfilePort authorizationProfiles;
    private final RefreshTokenRepository refreshTokens;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenGenerator tokenGenerator;
    private final Clock clock;
    private final AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();

    public RefreshAccessToken(
            UserRepository users,
            UserAccessRepository userAccess,
            AuthorizationProfilePort authorizationProfiles,
            RefreshTokenRepository refreshTokens,
            AccessTokenIssuer accessTokens,
            RefreshTokenGenerator tokenGenerator,
            Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.userAccess = Objects.requireNonNull(userAccess, "userAccess");
        this.authorizationProfiles = Objects.requireNonNull(authorizationProfiles, "authorizationProfiles");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.accessTokens = Objects.requireNonNull(accessTokens, "accessTokens");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public AuthenticationResult refresh(String refreshTokenValue) {
        Objects.requireNonNull(refreshTokenValue, "refreshTokenValue");
        Instant now = clock.instant();
        String hash = tokenGenerator.hashOf(refreshTokenValue);

        RefreshTokenRecord session = refreshTokens.findActiveByHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (session.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = users.findById(session.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        UserAccess access = userAccess.findAccessFor(user.id());
        // HU-IAM-010 E1 — recalculated on every refresh, same as the login path, so a role
        // change is reflected by the next refresh (authentication.md).
        List<String> featureScopes = authorizationPolicy.effectiveFeatureScopes(
                authorizationProfiles.loadProfile(user.id()), now);
        Instant expiresAt = now.plus(TokenPolicy.ACCESS_TOKEN_TTL);
        String accessToken = accessTokens.issue(user, access, featureScopes, session.id(), now, expiresAt);

        return new AuthenticationResult(
                accessToken,
                refreshTokenValue,
                TokenPolicy.ACCESS_TOKEN_TTL.toSeconds(),
                user,
                access);
    }
}
