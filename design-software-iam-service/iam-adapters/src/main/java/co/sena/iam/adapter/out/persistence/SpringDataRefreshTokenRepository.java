package co.sena.iam.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshTokenEntity> findByUserId(UUID userId);

    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);
}
