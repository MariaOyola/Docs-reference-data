package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.domain.model.RefreshTokenRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** In-memory double for unit tests (iam-core scope only). */
public final class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final List<RefreshTokenRecord> store = new ArrayList<>();

    @Override
    public void save(RefreshTokenRecord token) {
        store.removeIf(t -> t.id().equals(token.id()));
        store.add(token);
    }

    /** Saves a record that is already revoked (test fixture helper). */
    public void saveRevoked(RefreshTokenRecord token) {
        store.add(token);
    }

    @Override
    public Optional<RefreshTokenRecord> findActiveByHash(String tokenHash) {
        return store.stream()
                .filter(t -> t.tokenHash().equals(tokenHash) && !t.isRevoked())
                .findFirst();
    }

    @Override
    public Optional<RefreshTokenRecord> findById(UUID sessionId) {
        return store.stream().filter(t -> t.id().equals(sessionId)).findFirst();
    }

    @Override
    public void revoke(UUID sessionId, Instant revokedAt) {
        store.replaceAll(t -> t.id().equals(sessionId)
                ? new RefreshTokenRecord(t.id(), t.userId(), t.tokenHash(), t.deviceHint(),
                        t.ipAddress(), t.expiresAt(), t.createdAt(), true, revokedAt)
                : t);
    }

    @Override
    public List<RefreshTokenRecord> findByUserId(UUID userId) {
        return store.stream().filter(t -> t.userId().equals(userId)).toList();
    }

    @Override
    public void revokeAllByUserId(UUID userId, Instant revokedAt) {
        store.replaceAll(t -> t.userId().equals(userId) && !t.isRevoked()
                ? new RefreshTokenRecord(t.id(), t.userId(), t.tokenHash(), t.deviceHint(),
                        t.ipAddress(), t.expiresAt(), t.createdAt(), true, revokedAt)
                : t);
    }

    @Override
    public void revokeAllActiveForUser(UUID userId, Instant revokedAt) {
        store.replaceAll(t -> t.userId().equals(userId) && !t.isRevoked()
                ? new RefreshTokenRecord(t.id(), t.userId(), t.tokenHash(), t.deviceHint(),
                        t.ipAddress(), t.expiresAt(), t.createdAt(), true, revokedAt)
                : t);
    }

    public long activeCount(UUID userId) {
        return store.stream().filter(t -> t.userId().equals(userId) && !t.isRevoked()).count();
    }
}
