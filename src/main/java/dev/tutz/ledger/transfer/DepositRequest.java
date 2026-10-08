package dev.tutz.ledger.transfer;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
        @NotNull UUID accountId,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(max = 140) String description) {
}
