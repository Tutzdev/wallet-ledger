package dev.tutz.ledger.transfer;

public enum TransferType {
    DEPOSIT("deposit.posted"),
    WITHDRAWAL("withdrawal.posted"),
    TRANSFER("transfer.posted"),
    REVERSAL("reversal.posted");

    private final String eventType;

    TransferType(String eventType) {
        this.eventType = eventType;
    }

    public String eventType() {
        return eventType;
    }
}
