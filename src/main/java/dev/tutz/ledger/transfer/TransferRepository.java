package dev.tutz.ledger.transfer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByCreatedByAndIdempotencyKey(UUID createdBy, String idempotencyKey);

    boolean existsByReversalOfId(UUID reversalOfId);

    @Query("select t from Transfer t where t.sourceAccountId = :accountId or t.destinationAccountId = :accountId")
    Page<Transfer> findByAccount(UUID accountId, Pageable pageable);
}
