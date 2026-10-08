package dev.tutz.ledger.outbox;

/** Port to the outside world. Implementations throw when the message was not accepted. */
public interface EventPublisher {

    void publish(OutboxMessage message);
}
