package dev.tutz.ledger.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/** {@code ledger_outbox_pending} growing means the publisher is down: the first thing to alert on. */
@Component
class OutboxMetrics implements MeterBinder {

    private final OutboxRepository events;

    OutboxMetrics(OutboxRepository events) {
        this.events = events;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("ledger.outbox.pending", events, OutboxRepository::countByPublishedAtIsNull)
                .description("Outbox events not yet published")
                .register(registry);
    }
}
