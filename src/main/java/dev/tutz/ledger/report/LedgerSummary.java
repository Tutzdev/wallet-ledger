package dev.tutz.ledger.report;

import java.util.List;

public record LedgerSummary(
        long customerAccounts,
        long frozenAccounts,
        long transfersToday,
        List<CurrencyAmount> volumeToday,
        List<CurrencyAmount> heldByCustomers,
        long outboxPending,
        Long oldestPendingEventSeconds) {

    public LedgerSummary {
        volumeToday = List.copyOf(volumeToday);
        heldByCustomers = List.copyOf(heldByCustomers);
    }
}
