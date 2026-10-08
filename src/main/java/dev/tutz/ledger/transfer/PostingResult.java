package dev.tutz.ledger.transfer;

public record PostingResult(TransferResponse transfer, boolean replayed) {

    static PostingResult created(TransferResponse transfer) {
        return new PostingResult(transfer, false);
    }

    static PostingResult replayed(TransferResponse transfer) {
        return new PostingResult(transfer, true);
    }
}
