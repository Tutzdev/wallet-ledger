package dev.tutz.ledger.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * SKIP LOCKED lets several app instances drain the outbox in parallel:
     * each one takes a different batch instead of waiting on the same rows.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(Instant now, int limit);

    long countByPublishedAtIsNull();

    @Query("select min(o.createdAt) from OutboxEvent o where o.publishedAt is null")
    Optional<Instant> findOldestPendingCreatedAt();
}
