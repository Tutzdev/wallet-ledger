package dev.tutz.ledger.transfer;

import dev.tutz.ledger.money.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code amount} is signed: negative leaves the account, positive enters it. */
public record StatementLineResponse(
        UUID entryId,
        UUID transferId,
        TransferType type,
        BigDecimal amount,
        CurrencyCode currency,
        String description,
        Instant createdAt) {

    static StatementLineResponse from(StatementLine line) {
        return new StatementLineResponse(line.entryId(), line.transferId(), line.type(),
                line.currency().toDecimal(line.amountMinor()), line.currency(), line.description(), line.createdAt());
    }
}
