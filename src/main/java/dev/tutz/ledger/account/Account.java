package dev.tutz.ledger.account;

import dev.tutz.ledger.common.BusinessRuleException;
import dev.tutz.ledger.money.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * An account has no balance column on purpose: the balance is always the sum of its ledger entries,
 * so there is no second number that can drift away from the history.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Account() {
    }

    private Account(String ownerName, AccountType type, CurrencyCode currency, Instant now) {
        this.id = UUID.randomUUID();
        this.ownerName = ownerName;
        this.type = type;
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Account openCustomerAccount(String ownerName, CurrencyCode currency, Instant now) {
        return new Account(ownerName.strip(), AccountType.CUSTOMER, currency, now);
    }

    void changeStatus(AccountStatus newStatus, Instant now) {
        if (isSystem()) {
            throw new BusinessRuleException("SYSTEM_ACCOUNT", "System accounts cannot be frozen.");
        }
        status = newStatus;
        updatedAt = now;
    }

    public boolean isSystem() {
        return type == AccountType.SYSTEM;
    }

    public boolean isFrozen() {
        return status == AccountStatus.FROZEN;
    }

    /** System accounts mirror money outside the ledger, so their balance is expected to be negative. */
    public boolean canGoNegative() {
        return isSystem();
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public AccountType getType() {
        return type;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
