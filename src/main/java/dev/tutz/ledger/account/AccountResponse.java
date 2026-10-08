package dev.tutz.ledger.account;

import dev.tutz.ledger.money.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String ownerName,
        AccountType type,
        CurrencyCode currency,
        AccountStatus status,
        BigDecimal balance,
        Instant createdAt,
        Instant updatedAt) {

    static AccountResponse from(Account account, long balanceMinorUnits) {
        return new AccountResponse(account.getId(), account.getOwnerName(), account.getType(), account.getCurrency(),
                account.getStatus(), account.getCurrency().toDecimal(balanceMinorUnits), account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
