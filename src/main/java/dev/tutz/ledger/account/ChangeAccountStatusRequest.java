package dev.tutz.ledger.account;

import jakarta.validation.constraints.NotNull;

public record ChangeAccountStatusRequest(@NotNull AccountStatus status) {
}
