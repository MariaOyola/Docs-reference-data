package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.PasswordResetRecord;
import java.util.Optional;

/** Outbound port over {@code session.password_reset_request}. */
public interface PasswordResetRepository {

    void save(PasswordResetRecord record);

    /** Looks up the latest unused, non-expired request that matches {@code tokenHash}. */
    Optional<PasswordResetRecord> findActiveByTokenHash(String tokenHash);

    void markUsed(PasswordResetRecord record);
}
