package co.sena.iam.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    /** Case-insensitive, to match the {@code uq_user_email_lower} unique index. */
    @Query("select u from UserEntity u where lower(u.email) = lower(:email)")
    Optional<UserEntity> findByEmailIgnoringCase(@Param("email") String email);

    @Modifying
    @Query("""
            update UserEntity u
               set u.failedAttempts = 0,
                   u.lockedUntil = null,
                   u.lastLoginAt = :at,
                   u.updatedAt = :at
             where u.id = :id
            """)
    int clearLockoutAndStampLogin(@Param("id") UUID id, @Param("at") OffsetDateTime at);

    @Modifying
    @Query("""
            update UserEntity u
               set u.failedAttempts = :attempts,
                   u.lockedUntil = :lockedUntil,
                   u.updatedAt = :at
             where u.id = :id
            """)
    int updateLockout(
            @Param("id") UUID id,
            @Param("attempts") short attempts,
            @Param("lockedUntil") OffsetDateTime lockedUntil,
            @Param("at") OffsetDateTime at);

    @Modifying
    @Query("""
            update UserEntity u
               set u.passwordHash = :hash,
                   u.mustChangePassword = false,
                   u.temporaryPasswordExpiresAt = null,
                   u.updatedAt = :at
             where u.id = :id
            """)
    int updatePasswordAndClearMustChange(
            @Param("id") UUID id,
            @Param("hash") String hash,
            @Param("at") OffsetDateTime at);
}
