package dev.tutz.ledger.money;

import java.math.BigDecimal;
import java.util.Currency;

public enum CurrencyCode {
    BRL,
    USD,
    EUR;

    public int fractionDigits() {
        return Currency.getInstance(name()).getDefaultFractionDigits();
    }

    public BigDecimal toDecimal(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, fractionDigits());
    }
}
