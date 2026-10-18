package co.sena.iam.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataPasswordResetRepository extends JpaRepository<PasswordResetRequestEntity, UUID> {

    @Query("""
            select r from PasswordResetRequestEntity r
             where r.tokenHash = :hash
               and r.used = false
               and r.expiresAt > :now
            """)
    Optional<PasswordResetRequestEntity> findActiveByTokenHash(
            @Param("hash") String hash,
            @Param("now") OffsetDateTime now);
}
