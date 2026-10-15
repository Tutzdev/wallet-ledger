package dev.tutz.ledger.transfer;

import dev.tutz.ledger.account.Account;
import dev.tutz.ledger.common.AssignedIdEntity;
import dev.tutz.ledger.money.CurrencyCode;
import dev.tutz.ledger.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** One money movement. Immutable in the code and append-only in the database (see the V3 trigger). */
@Entity
@Immutable
@Table(name = "transfers")
public class Transfer extends AssignedIdEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferType type;

    @Column(nullable = false)
    private UUID sourceAccountId;

    @Column(nullable = false)
    private UUID destinationAccountId;

    @Column(nullable = false)
    private long amountMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(length = 140)
    private String description;

    private UUID reversalOfId;

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    Transfer(TransferCommand command, Account source, Account destination, Money amount, Instant now) {
        this.id = UUID.randomUUID();
        this.type = command.type();
        this.sourceAccountId = source.getId();
        this.destinationAccountId = destination.getId();
        this.amountMinor = amount.minorUnits();
        this.currency = amount.currency();
        this.description = command.description();
        this.reversalOfId = command.reversalOfId();
        this.idempotencyKey = command.idempotencyKey();
        this.requestFingerprint = command.fingerprint();
        this.createdBy = command.operatorId();
        this.createdAt = now;
    }

    boolean wasRequestedWith(String fingerprint) {
        return requestFingerprint.equals(fingerprint);
    }

    Money amount() {
        return new Money(amountMinor, currency);
    }

    @Override
    public UUID getId() {
        return id;
    }

    public TransferType getType() {
        return type;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public UUID getReversalOfId() {
        return reversalOfId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
