package dev.tutz.ledger.transfer;

import dev.tutz.ledger.money.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload published through the outbox. It is a public contract, so it never exposes the entity.
 *
 * @param idempotencyKey the key the operator sent with the request. A client that lost the HTTP answer
 *        does not know the transfer id, but it does know its own key, so this is how it recognises the event.
 *        Keys are unique per operator, not globally.
 */
public record TransferPostedEvent(
        UUID transferId,
        TransferType type,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        CurrencyCode currency,
        String description,
        UUID reversalOfId,
        String idempotencyKey,
        Instant postedAt) {

    static TransferPostedEvent from(Transfer transfer) {
        return new TransferPostedEvent(transfer.getId(), transfer.getType(), transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(), transfer.getCurrency().toDecimal(transfer.getAmountMinor()),
                transfer.getCurrency(), transfer.getDescription(), transfer.getReversalOfId(), transfer.getIdempotencyKey(),
                transfer.getCreatedAt());
    }
}
