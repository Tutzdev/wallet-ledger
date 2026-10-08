package dev.tutz.ledger.outbox;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ledger.events")
public record EventProperties(
        @DefaultValue("log") String publisher,
        @DefaultValue Kafka kafka,
        @DefaultValue Relay relay) {

    public record Kafka(@DefaultValue("ledger.transfers") String topic) {
    }

    public record Relay(
            @DefaultValue("http://localhost:8000") URI url,
            String apiKey,
            @DefaultValue("5s") Duration timeout) {
    }
}
