package dev.tutz.ledger.account;

import dev.tutz.ledger.money.CurrencyCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String ownerName,
        @NotNull CurrencyCode currency) {
}
