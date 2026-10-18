package co.sena.iam.adapter.out.persistence;

import co.sena.iam.application.port.out.LoginAuditRepository;
import co.sena.iam.domain.model.LoginAttempt;
import jakarta.persistence.EntityManager;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbound adapter for {@code identity_audit.audit_login}.
 *
 * <p>{@code REQUIRES_NEW}: the trail has to survive whatever the caller does next, and a
 * refused login always ends in an exception.
 */
@Repository
class JpaLoginAuditRepository implements LoginAuditRepository {

    private final EntityManager entityManager;

    JpaLoginAuditRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LoginAttempt attempt) {
        entityManager.persist(new AuditLoginEntity(
                UUID.randomUUID(),
                attempt.userId(),
                attempt.emailAttempted(),
                attempt.outcome().name(),
                attempt.ipAddress(),
                attempt.userAgent(),
                attempt.attemptedAt().atOffset(ZoneOffset.UTC)));
    }
}
