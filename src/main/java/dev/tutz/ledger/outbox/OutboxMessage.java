package dev.tutz.ledger.outbox;

import java.time.Instant;
import java.util.UUID;

/** What a publisher receives. {@code id} is stable across retries, so consumers can deduplicate. */
public record OutboxMessage(UUID id, UUID aggregateId, String eventType, String payload, Instant occurredAt) {

    static OutboxMessage from(OutboxEvent event) {
        return new OutboxMessage(event.getId(), event.getAggregateId(), event.getEventType(), event.getPayload(),
                event.getCreatedAt());
    }
}
