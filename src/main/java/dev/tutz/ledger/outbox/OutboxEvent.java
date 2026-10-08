package dev.tutz.ledger.outbox;

import dev.tutz.ledger.common.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends AssignedIdEntity {

    private static final int MAX_ERROR_LENGTH = 300;

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    protected OutboxEvent() {
    }

    OutboxEvent(UUID aggregateId, String eventType, String payload, Instant now) {
        this.id = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = now;
        this.nextAttemptAt = now;
    }

    void markPublished(Instant now) {
        publishedAt = now;
        attempts++;
        lastError = null;
    }

    void markFailed(String error, Instant retryAt) {
        attempts++;
        nextAttemptAt = retryAt;
        lastError = error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getAttempts() {
        return attempts;
    }
}
