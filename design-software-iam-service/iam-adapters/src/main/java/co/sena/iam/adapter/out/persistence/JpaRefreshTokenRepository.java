package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.domain.model.RefreshTokenRecord;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Outbound adapter for {@code session.refresh_token}. */
@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final EntityManager entityManager;
    private final SpringDataRefreshTokenRepository jpaRepo;

    JpaRefreshTokenRepository(EntityManager entityManager, SpringDataRefreshTokenRepository jpaRepo) {
        this.entityManager = entityManager;
        this.jpaRepo = jpaRepo;
    }

    @Override
    @Transactional
    public void save(RefreshTokenRecord token) {
        entityManager.persist(new RefreshTokenEntity(
                token.id(),
                token.userId(),
                token.tokenHash(),
                token.deviceHint(),
                token.ipAddress(),
                token.expiresAt().atOffset(ZoneOffset.UTC),
                token.createdAt().atOffset(ZoneOffset.UTC)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshTokenRecord> findActiveByHash(String tokenHash) {
        return jpaRepo.findByTokenHashAndRevokedFalse(tokenHash)
                .map(RefreshTokenEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshTokenRecord> findById(UUID sessionId) {
        return jpaRepo.findById(sessionId).map(RefreshTokenEntity::toDomain);
    }

    @Override
    @Transactional
    public void revoke(UUID sessionId, Instant revokedAt) {
        jpaRepo.findById(sessionId).ifPresent(entity -> {
            entity.revoke(OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC));
        });
    }

    @Override
    @Transactional
    public void revokeAllActiveForUser(UUID userId, Instant revokedAt) {
        OffsetDateTime at = OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC);
        jpaRepo.findByUserIdAndRevokedFalse(userId).forEach(entity -> entity.revoke(at));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshTokenRecord> findByUserId(UUID userId) {
        return jpaRepo.findByUserId(userId).stream()
                .map(RefreshTokenEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId, Instant revokedAt) {
        jpaRepo.findByUserIdAndRevokedFalse(userId).forEach(entity ->
                entity.revoke(OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC)));
    }
}
