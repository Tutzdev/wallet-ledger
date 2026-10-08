package dev.tutz.ledger.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.tutz.ledger.common.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void convertsDecimalToMinorUnitsExactly() {
        Money money = Money.of(new BigDecimal("10.5"), CurrencyCode.BRL);

        assertThat(money.minorUnits()).isEqualTo(1050);
        assertThat(money.toDecimal()).isEqualByComparingTo("10.50");
    }

    @Test
    void rejectsFractionsOfACent() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("0.001"), CurrencyCode.USD))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_AMOUNT");
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ZERO, CurrencyCode.EUR)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1"), CurrencyCode.EUR)).isInstanceOf(BusinessRuleException.class);
    }
}
