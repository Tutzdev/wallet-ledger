package dev.tutz.ledger.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A reversal always records why it happened, for the audit trail. */
public record ReversalRequest(@NotBlank @Size(max = 140) String reason) {
}
