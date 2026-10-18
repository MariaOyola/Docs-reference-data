package co.sena.iam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Row of {@code identity.user}. The table name is a reserved word, hence the quoting. */
@Entity
@Table(name = "\"user\"", schema = "identity")
class UserEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "actor_type", nullable = false, length = 20)
    private String actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "password_temporary_expires_at")
    private OffsetDateTime temporaryPasswordExpiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserEntity() {
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getFirstName() {
        return firstName;
    }

    String getLastName() {
        return lastName;
    }

    String getActorType() {
        return actorType;
    }

    UUID getActorId() {
        return actorId;
    }

    boolean isActive() {
        return active;
    }

    short getFailedAttempts() {
        return failedAttempts;
    }

    OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    boolean isMustChangePassword() {
        return mustChangePassword;
    }

    OffsetDateTime getTemporaryPasswordExpiresAt() {
        return temporaryPasswordExpiresAt;
    }

    OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
