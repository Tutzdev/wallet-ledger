package dev.tutz.ledger.report;

import dev.tutz.ledger.money.CurrencyCode;
import java.math.BigDecimal;

public record CurrencyAmount(CurrencyCode currency, BigDecimal amount) {

    static CurrencyAmount of(String currency, long minorUnits) {
        CurrencyCode code = CurrencyCode.valueOf(currency);
        return new CurrencyAmount(code, code.toDecimal(minorUnits));
    }
}
