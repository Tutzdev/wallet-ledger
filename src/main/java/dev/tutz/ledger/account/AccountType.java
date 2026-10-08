package dev.tutz.ledger.account;

public enum AccountType {
    CUSTOMER,
    /** Counterpart of deposits and withdrawals: the money that lives outside the ledger. */
    SYSTEM
}
