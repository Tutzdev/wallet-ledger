package dev.tutz.ledger.outbox;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

/**
 * Sends events to webhook-relay, which fans them out to the subscribers' endpoints.
 * The outbox id goes as idempotency_key, so a retried publish never creates a duplicate event.
 */
@Component
@ConditionalOnProperty(name = "ledger.events.publisher", havingValue = "relay")
class RelayEventPublisher implements EventPublisher {

    private final RestClient client;
    private final ObjectMapper json;

    RelayEventPublisher(EventProperties properties, ObjectMapper json) {
        EventProperties.Relay relay = properties.relay();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(relay.timeout());
        requestFactory.setReadTimeout(relay.timeout());

        this.client = RestClient.builder()
                .baseUrl(relay.url().toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + relay.apiKey())
                .build();
        this.json = json;
    }

    @Override
    public void publish(OutboxMessage message) {
        String body = json.writeValueAsString(Map.of(
                "event_type", message.eventType(),
                "payload", json.readTree(message.payload()),
                "idempotency_key", message.id().toString()));
        try {
            client.post()
                    .uri("/api/v1/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EventPublishingException("Relay rejected the event", exception);
        }
    }
}
