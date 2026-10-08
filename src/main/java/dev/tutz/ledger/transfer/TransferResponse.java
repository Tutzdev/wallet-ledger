package dev.tutz.ledger.transfer;

import dev.tutz.ledger.money.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        TransferType type,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        CurrencyCode currency,
        String description,
        UUID reversalOfId,
        Instant createdAt) {

    static TransferResponse from(Transfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getType(), transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(), transfer.getCurrency().toDecimal(transfer.getAmountMinor()),
                transfer.getCurrency(), transfer.getDescription(), transfer.getReversalOfId(), transfer.getCreatedAt());
    }
}
