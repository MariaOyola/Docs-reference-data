package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.domain.model.OutboxEvent;
import java.util.ArrayList;
import java.util.List;

public final class RecordingOutboxPort implements OutboxPort {

    private final List<OutboxEvent> staged = new ArrayList<>();

    @Override
    public void stage(OutboxEvent event) {
        staged.add(event);
    }

    public OutboxEvent singleEvent() {
        if (staged.size() != 1) {
            throw new AssertionError("Expected exactly 1 staged event, got " + staged.size());
        }
        return staged.get(0);
    }

    public List<OutboxEvent> all() {
        return List.copyOf(staged);
    }

    public boolean isEmpty() {
        return staged.isEmpty();
    }
}
