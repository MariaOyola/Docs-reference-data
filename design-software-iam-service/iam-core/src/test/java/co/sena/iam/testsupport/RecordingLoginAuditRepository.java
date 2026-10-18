package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.LoginAuditRepository;
import co.sena.iam.domain.model.LoginAttempt;
import co.sena.iam.domain.model.LoginOutcome;
import java.util.ArrayList;
import java.util.List;

/** Keeps every audited attempt so tests can assert on the trail. */
public final class RecordingLoginAuditRepository implements LoginAuditRepository {

    private final List<LoginAttempt> attempts = new ArrayList<>();

    @Override
    public void record(LoginAttempt attempt) {
        attempts.add(attempt);
    }

    public List<LoginAttempt> attempts() {
        return List.copyOf(attempts);
    }

    public List<LoginOutcome> outcomes() {
        return attempts.stream().map(LoginAttempt::outcome).toList();
    }

    public LoginAttempt last() {
        return attempts.get(attempts.size() - 1);
    }
}
