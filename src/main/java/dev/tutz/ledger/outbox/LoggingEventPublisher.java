package dev.tutz.ledger.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default for local runs: no broker needed, every event shows up in the log. */
@Component
@ConditionalOnProperty(name = "ledger.events.publisher", havingValue = "log", matchIfMissing = true)
class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(OutboxMessage message) {
        log.info("event={} id={} aggregate={}", message.eventType(), message.id(), message.aggregateId());
    }
}
