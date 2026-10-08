package dev.tutz.ledger.transfer;

import dev.tutz.ledger.common.AssignedIdEntity;
import dev.tutz.ledger.money.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * One side of a transfer. Every transfer writes exactly one debit (negative) and one credit (positive)
 * of the same amount, so the entries of any transfer, and of the whole ledger, always sum to zero.
 */
@Entity
@Immutable
@Table(name = "ledger_entries")
public class LedgerEntry extends AssignedIdEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID transferId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private long amountMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(Transfer transfer, UUID accountId, long signedAmount) {
        this.id = UUID.randomUUID();
        this.transferId = transfer.getId();
        this.accountId = accountId;
        this.amountMinor = signedAmount;
        this.currency = transfer.getCurrency();
        this.createdAt = transfer.getCreatedAt();
    }

    static LedgerEntry debit(Transfer transfer) {
        return new LedgerEntry(transfer, transfer.getSourceAccountId(), -transfer.getAmountMinor());
    }

    static LedgerEntry credit(Transfer transfer) {
        return new LedgerEntry(transfer, transfer.getDestinationAccountId(), transfer.getAmountMinor());
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }
}
