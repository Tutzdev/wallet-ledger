package dev.tutz.ledger.account;

public enum AccountStatus {
    ACTIVE,
    /** Can still receive money, but cannot send it. */
    FROZEN
}
