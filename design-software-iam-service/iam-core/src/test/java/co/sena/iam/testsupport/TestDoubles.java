package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.AccessTokenIssuer;
import co.sena.iam.application.port.out.AuthorizationProfilePort;
import co.sena.iam.application.port.out.PasswordHasher;
import co.sena.iam.application.port.out.RefreshTokenGenerator;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.application.port.out.UserAccessRepository;
import co.sena.iam.domain.model.AuthorizationProfile;
import co.sena.iam.domain.model.RefreshTokenRecord;
import co.sena.iam.domain.model.RefreshTokenSecret;
import co.sena.iam.domain.model.UserAccess;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Small hand-written doubles for the outbound ports the use cases talk to. */
public final class TestDoubles {

    private TestDoubles() {
    }

    /** "Hashing" is a prefix so tests can tell a right password from a wrong one. */
    public static final String HASH_PREFIX = "hashed:";

    public static PasswordHasher passwordHasher() {
        return new PasswordHasher() {
            @Override
            public boolean matches(String raw, String hash) {
                return (HASH_PREFIX + raw).equals(hash);
            }

            @Override
            public String hash(String raw) {
                return HASH_PREFIX + raw;
            }
        };
    }

    public static String hashOf(String rawPassword) {
        return HASH_PREFIX + rawPassword;
    }

    public static AccessTokenIssuer accessTokenIssuer(String token) {
        return (user, access, featureScopes, sessionId, issuedAt, expiresAt) -> token;
    }

    /** Captures the {@code featureScopes} it was last called with, so tests can assert on them. */
    public static final class RecordingAccessTokenIssuer implements AccessTokenIssuer {

        private final String token;
        private List<String> lastFeatureScopes;

        public RecordingAccessTokenIssuer(String token) {
            this.token = token;
        }

        @Override
        public String issue(
                co.sena.iam.domain.model.User user, UserAccess access, List<String> featureScopes,
                UUID sessionId, Instant issuedAt, Instant expiresAt) {
            this.lastFeatureScopes = featureScopes;
            return token;
        }

        public List<String> lastFeatureScopes() {
            return lastFeatureScopes;
        }
    }

    public static RefreshTokenGenerator refreshTokenGenerator(String value) {
        return new RefreshTokenGenerator() {
            @Override
            public RefreshTokenSecret generate() {
                return new RefreshTokenSecret(value, "digest-of-" + value);
            }

            @Override
            public String hashOf(String v) {
                return "digest-of-" + v;
            }
        };
    }

    public static UserAccessRepository userAccess(UserAccess access) {
        return userId -> access;
    }

    public static AuthorizationProfilePort authorizationProfile(AuthorizationProfile profile) {
        return userId -> profile;
    }

    public static AuthorizationProfilePort emptyAuthorizationProfile() {
        return userId -> AuthorizationProfile.empty();
    }

    /** Collects saved refresh tokens. */
    public static final class RecordingRefreshTokenRepository implements RefreshTokenRepository {

        private final List<RefreshTokenRecord> saved = new ArrayList<>();

        @Override
        public void save(RefreshTokenRecord token) {
            saved.add(token);
        }

        public List<RefreshTokenRecord> saved() {
            return List.copyOf(saved);
        }

        @Override
        public Optional<RefreshTokenRecord> findActiveByHash(String tokenHash) {
            return saved.stream().filter(t -> t.tokenHash().equals(tokenHash) && !t.isRevoked()).findFirst();
        }

        @Override
        public Optional<RefreshTokenRecord> findById(UUID sessionId) {
            return saved.stream().filter(t -> t.id().equals(sessionId)).findFirst();
        }

        @Override
        public void revoke(UUID sessionId, Instant revokedAt) {
        }

        @Override
        public void revokeAllActiveForUser(UUID userId, Instant revokedAt) {
        }

        @Override
        public List<RefreshTokenRecord> findByUserId(UUID userId) {
            return saved.stream().filter(t -> t.userId().equals(userId)).toList();
        }

        @Override
        public void revokeAllByUserId(UUID userId, Instant revokedAt) {
            revokedUserIds.add(userId);
        }

        public boolean wereSessionsRevokedFor(UUID userId) {
            return revokedUserIds.contains(userId);
        }

        private final java.util.Set<UUID> revokedUserIds = new java.util.HashSet<>();
    }

    /** Fails the test if a refresh token is persisted on a path that must not create a session. */
    public static RefreshTokenRepository refuseToSave() {
        return new RefreshTokenRepository() {
            @Override
            public void save(RefreshTokenRecord token) {
                throw new AssertionError("no session must be created for user " + token.userId());
            }

            @Override
            public Optional<RefreshTokenRecord> findActiveByHash(String tokenHash) {
                return Optional.empty();
            }

            @Override
            public Optional<RefreshTokenRecord> findById(UUID sessionId) {
                return Optional.empty();
            }

            @Override
            public void revoke(UUID sessionId, Instant revokedAt) {
            }

            @Override
            public void revokeAllActiveForUser(UUID userId, Instant revokedAt) {
            }

            @Override
            public List<RefreshTokenRecord> findByUserId(UUID userId) {
                return List.of();
            }

            @Override
            public void revokeAllByUserId(UUID userId, Instant revokedAt) {
            }
        };
    }

    public static UUID uuid(String last) {
        return UUID.fromString("00000000-0000-0000-0000-0000000000" + last);
    }
}
