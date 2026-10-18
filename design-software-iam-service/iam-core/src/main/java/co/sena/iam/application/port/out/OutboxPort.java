package co.sena.iam.application.port.out;

import co.sena.iam.domain.model.OutboxEvent;

/** Stages a domain event in {@code identity.outbox} within the current transaction. */
public interface OutboxPort {

    void stage(OutboxEvent event);
}
