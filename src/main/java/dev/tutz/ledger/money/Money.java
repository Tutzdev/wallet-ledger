package dev.tutz.ledger.money;

import dev.tutz.ledger.common.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A positive amount kept in minor units (cents). Floating point never touches money:
 * the decimal that comes from the API is converted once, exactly, or rejected.
 */
public record Money(long minorUnits, CurrencyCode currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (minorUnits <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "Amount must be greater than zero.");
        }
    }

    public static Money of(BigDecimal amount, CurrencyCode currency) {
        Objects.requireNonNull(amount, "amount");
        try {
            BigDecimal minorUnits = amount.setScale(currency.fractionDigits(), RoundingMode.UNNECESSARY)
                    .movePointRight(currency.fractionDigits());

            return new Money(minorUnits.longValueExact(), currency);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("INVALID_AMOUNT",
                    "Amount must have at most %d decimal places for %s.".formatted(currency.fractionDigits(), currency));
        }
    }

    public BigDecimal toDecimal() {
        return currency.toDecimal(minorUnits);
    }
}
