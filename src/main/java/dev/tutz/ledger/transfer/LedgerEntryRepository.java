package dev.tutz.ledger.transfer;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransferId(UUID transferId);

    @Query(value = """
            select new dev.tutz.ledger.transfer.StatementLine(
                e.id, e.transferId, t.type, e.amountMinor, e.currency, t.description, e.createdAt)
            from LedgerEntry e join Transfer t on t.id = e.transferId
            where e.accountId = :accountId
            """,
            countQuery = "select count(e) from LedgerEntry e where e.accountId = :accountId")
    Page<StatementLine> findStatement(UUID accountId, Pageable pageable);
}
