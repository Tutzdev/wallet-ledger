package dev.tutz.ledger.transfer;

import dev.tutz.ledger.common.BusinessRuleException;
import dev.tutz.ledger.common.PageResponse;
import dev.tutz.ledger.common.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entry point for money movement. It makes every request idempotent: the same Idempotency-Key
 * returns the transfer that was already posted instead of moving the money twice.
 */
@Service
public class TransferService {

    private final TransferPoster poster;
    private final TransferRepository transfers;
    private final MeterRegistry meters;

    public TransferService(TransferPoster poster, TransferRepository transfers, MeterRegistry meters) {
        this.poster = poster;
        this.transfers = transfers;
        this.meters = meters;
    }

    public PostingResult submit(TransferCommand command) {
        Optional<PostingResult> replay = replayOf(command);
        if (replay.isPresent()) {
            return replay.get();
        }

        try {
            Transfer transfer = poster.post(command);
            meters.counter("ledger.transfers.posted", "type", transfer.getType().name()).increment();

            return PostingResult.created(TransferResponse.from(transfer));
        } catch (DataIntegrityViolationException exception) {
            // Lost a race: a request with the same key, or a reversal of the same transfer, committed first.
            return replayOf(command).orElseThrow(() -> conflictFor(command, exception));
        }
    }

    @Transactional(readOnly = true)
    public TransferResponse find(UUID transferId) {
        return transfers.findById(transferId)
                .map(TransferResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer %s not found.".formatted(transferId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> search(UUID accountId, Pageable pageable) {
        var page = accountId == null ? transfers.findAll(pageable) : transfers.findByAccount(accountId, pageable);

        return PageResponse.from(page.map(TransferResponse::from));
    }

    private Optional<PostingResult> replayOf(TransferCommand command) {
        return transfers.findByCreatedByAndIdempotencyKey(command.operatorId(), command.idempotencyKey())
                .map(existing -> {
                    if (!existing.wasRequestedWith(command.fingerprint())) {
                        throw new BusinessRuleException("IDEMPOTENCY_KEY_REUSED",
                                "This Idempotency-Key was already used for a different request.");
                    }
                    meters.counter("ledger.transfers.replayed").increment();

                    return PostingResult.replayed(TransferResponse.from(existing));
                });
    }

    private static RuntimeException conflictFor(TransferCommand command, DataIntegrityViolationException exception) {
        if (command.type() == TransferType.REVERSAL) {
            return new BusinessRuleException("ALREADY_REVERSED", "Transfer %s was already reversed.".formatted(command.reversalOfId()));
        }
        return exception;
    }
}
