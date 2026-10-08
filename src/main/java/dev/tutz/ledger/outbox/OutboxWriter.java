package dev.tutz.ledger.outbox;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Saves the event in the same database transaction as the change that caused it.
 * Either both are committed or neither is, so no event is ever lost or invented.
 */
@Component
public class OutboxWriter {

    private final OutboxRepository events;
    private final ObjectMapper json;
    private final Clock clock;

    public OutboxWriter(OutboxRepository events, ObjectMapper json, Clock clock) {
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String eventType, UUID aggregateId, Object payload) {
        events.save(new OutboxEvent(aggregateId, eventType, json.writeValueAsString(payload), clock.instant()));
    }
}
