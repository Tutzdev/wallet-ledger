package dev.tutz.ledger.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ledger.outbox")
public record OutboxProperties(
        @DefaultValue("50") int batchSize,
        @DefaultValue("5m") Duration maxBackoff) {
}
