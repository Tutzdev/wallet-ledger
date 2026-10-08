package dev.tutz.ledger.outbox;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Polls the outbox and hands each event to the configured publisher. Delivery is at-least-once:
 * an event can be sent twice if the app dies between publishing and marking it, so consumers
 * deduplicate by the event id.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int MAX_BACKOFF_EXPONENT = 10;

    private final OutboxRepository events;
    private final EventPublisher publisher;
    private final TransactionTemplate transactions;
    private final OutboxProperties properties;
    private final Clock clock;

    OutboxRelay(OutboxRepository events, EventPublisher publisher, TransactionTemplate transactions,
            OutboxProperties properties, Clock clock) {
        this.events = events;
        this.publisher = publisher;
        this.transactions = transactions;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${ledger.outbox.poll-interval-ms:1000}")
    void publishPending() {
        transactions.executeWithoutResult(status ->
                events.lockNextBatch(clock.instant(), properties.batchSize()).forEach(this::publish));
    }

    private void publish(OutboxEvent event) {
        try {
            publisher.publish(OutboxMessage.from(event));
            event.markPublished(clock.instant());
        } catch (RuntimeException exception) {
            Duration delay = backoffFor(event.getAttempts() + 1);
            event.markFailed(exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    clock.instant().plus(delay));
            log.warn("Outbox event {} not published (attempt {}), retrying in {}s",
                    event.getId(), event.getAttempts(), delay.toSeconds());
        }
    }

    /** 2s, 4s, 8s... capped, so a broker outage does not turn into a busy loop. */
    private Duration backoffFor(int attempt) {
        Duration exponential = Duration.ofSeconds(1L << Math.min(attempt, MAX_BACKOFF_EXPONENT));

        return exponential.compareTo(properties.maxBackoff()) > 0 ? properties.maxBackoff() : exponential;
    }
}
