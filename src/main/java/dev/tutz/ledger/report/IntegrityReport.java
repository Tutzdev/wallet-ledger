package dev.tutz.ledger.report;

import java.util.List;

/**
 * {@code balanced} is the double-entry invariant: every transfer's entries, and the
 * whole ledger per currency, add up to exactly zero.
 */
public record IntegrityReport(boolean balanced, long unbalancedTransfers, List<CurrencyAmount> netByCurrency) {

    public IntegrityReport {
        netByCurrency = List.copyOf(netByCurrency);
    }
}
