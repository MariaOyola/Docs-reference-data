package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.PasswordResetRepository;
import co.sena.iam.domain.model.PasswordResetRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** In-memory stand-in for {@code session.password_reset_request}. */
public final class InMemoryPasswordResetRepository implements PasswordResetRepository {

    private final List<PasswordResetRecord> records = new ArrayList<>();

    @Override
    public void save(PasswordResetRecord record) {
        records.add(record);
    }

    @Override
    public Optional<PasswordResetRecord> findActiveByTokenHash(String tokenHash) {
        Instant now = Instant.now();
        return records.stream()
                .filter(r -> r.tokenHash().equals(tokenHash) && !r.used() && !r.isExpiredAt(now))
                .findFirst();
    }

    @Override
    public void markUsed(PasswordResetRecord record) {
        int idx = records.indexOf(record);
        if (idx >= 0) {
            records.set(idx, new PasswordResetRecord(
                    record.id(), record.userId(), record.tokenHash(),
                    record.expiresAt(), true, record.requestedAt(), record.ipAddress()));
        }
    }

    public List<PasswordResetRecord> all() {
        return List.copyOf(records);
    }

    public Optional<PasswordResetRecord> findByTokenHash(String hash) {
        return records.stream().filter(r -> r.tokenHash().equals(hash)).findFirst();
    }
}
