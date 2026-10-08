package dev.tutz.ledger.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@ConditionalOnProperty(name = "ledger.events.publisher", havingValue = "kafka")
class KafkaEventPublisher implements EventPublisher {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);
    private static final int PARTITIONS = 3;

    private final KafkaTemplate<String, String> kafka;
    private final EventProperties properties;

    KafkaEventPublisher(KafkaTemplate<String, String> kafka, EventProperties properties) {
        this.kafka = kafka;
        this.properties = properties;
    }

    @Bean
    NewTopic ledgerTopic() {
        return TopicBuilder.name(properties.kafka().topic()).partitions(PARTITIONS).replicas(1).build();
    }

    /** Waits for the broker acknowledgement: the outbox row is only marked once Kafka has the event. */
    @Override
    public void publish(OutboxMessage message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(properties.kafka().topic(),
                message.aggregateId().toString(), message.payload());
        record.headers()
                .add("event-id", bytes(message.id().toString()))
                .add("event-type", bytes(message.eventType()));
        try {
            kafka.send(record).get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException("Interrupted while publishing to Kafka", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new EventPublishingException("Kafka did not acknowledge the event", exception);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
