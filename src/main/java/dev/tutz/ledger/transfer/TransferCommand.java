package dev.tutz.ledger.transfer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * What a client asked the ledger to do. Account ids that the ledger resolves by itself
 * (the system account of a deposit, the sides of a reversal) stay null here.
 */
public record TransferCommand(
        TransferType type,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String description,
        UUID reversalOfId,
        String idempotencyKey,
        UUID operatorId) {

    public TransferCommand {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(operatorId, "operatorId");
        description = StringUtils.hasText(description) ? description.strip() : null;
    }

    public static TransferCommand deposit(UUID accountId, BigDecimal amount, String description,
            String idempotencyKey, UUID operatorId) {
        return new TransferCommand(TransferType.DEPOSIT, null, accountId, amount, description, null,
                idempotencyKey, operatorId);
    }

    public static TransferCommand withdrawal(UUID accountId, BigDecimal amount, String description,
            String idempotencyKey, UUID operatorId) {
        return new TransferCommand(TransferType.WITHDRAWAL, accountId, null, amount, description, null,
                idempotencyKey, operatorId);
    }

    public static TransferCommand transfer(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
            String description, String idempotencyKey, UUID operatorId) {
        return new TransferCommand(TransferType.TRANSFER, sourceAccountId, destinationAccountId, amount, description,
                null, idempotencyKey, operatorId);
    }

    public static TransferCommand reversal(UUID transferId, String reason, String idempotencyKey, UUID operatorId) {
        return new TransferCommand(TransferType.REVERSAL, null, null, null, reason, transferId,
                idempotencyKey, operatorId);
    }

    /** SHA-256 of the request. A reused Idempotency-Key is only accepted with the very same request. */
    String fingerprint() {
        String canonical = String.join("|",
                type.name(),
                String.valueOf(sourceAccountId),
                String.valueOf(destinationAccountId),
                amount == null ? "" : amount.stripTrailingZeros().toPlainString(),
                Objects.toString(description, ""),
                String.valueOf(reversalOfId));

        return HexFormat.of().formatHex(sha256(canonical));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by every JVM", exception);
        }
    }
}
