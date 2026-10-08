package dev.tutz.ledger.transfer;

import dev.tutz.ledger.money.CurrencyCode;
import java.time.Instant;
import java.util.UUID;

/** JPQL projection of an entry joined with its transfer. */
public record StatementLine(
        UUID entryId,
        UUID transferId,
        TransferType type,
        long amountMinor,
        CurrencyCode currency,
        String description,
        Instant createdAt) {
}
