package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.PasswordResetRepository;
import co.sena.iam.domain.model.PasswordResetRecord;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Outbound adapter for {@code session.password_reset_request}. */
@Repository
class JpaPasswordResetRepository implements PasswordResetRepository {

    private final SpringDataPasswordResetRepository jpaRepo;

    JpaPasswordResetRepository(SpringDataPasswordResetRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    @Transactional
    public void save(PasswordResetRecord record) {
        jpaRepo.save(new PasswordResetRequestEntity(record));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetRecord> findActiveByTokenHash(String tokenHash) {
        return jpaRepo.findActiveByTokenHash(tokenHash, OffsetDateTime.now(ZoneOffset.UTC))
                .map(PasswordResetRequestEntity::toDomain);
    }

    @Override
    @Transactional
    public void markUsed(PasswordResetRecord record) {
        jpaRepo.findById(record.id()).ifPresent(PasswordResetRequestEntity::markUsed);
    }
}
