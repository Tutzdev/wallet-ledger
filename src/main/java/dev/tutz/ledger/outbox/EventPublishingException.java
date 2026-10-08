package dev.tutz.ledger.outbox;

public class EventPublishingException extends RuntimeException {

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
