package dev.tutz.ledger.transfer;

import dev.tutz.ledger.auth.CurrentOperator;
import dev.tutz.ledger.common.PageRequests;
import dev.tutz.ledger.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TransferController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String KEY_FORMAT = "[A-Za-z0-9_-]{8,64}";

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping("/deposits")
    public ResponseEntity<TransferResponse> deposit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(IDEMPOTENCY_KEY) @Pattern(regexp = KEY_FORMAT) String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        return respond(transfers.submit(TransferCommand.deposit(request.accountId(), request.amount(),
                request.description(), idempotencyKey, CurrentOperator.idOf(jwt))));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<TransferResponse> withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(IDEMPOTENCY_KEY) @Pattern(regexp = KEY_FORMAT) String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request) {
        return respond(transfers.submit(TransferCommand.withdrawal(request.accountId(), request.amount(),
                request.description(), idempotencyKey, CurrentOperator.idOf(jwt))));
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(IDEMPOTENCY_KEY) @Pattern(regexp = KEY_FORMAT) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return respond(transfers.submit(TransferCommand.transfer(request.sourceAccountId(),
                request.destinationAccountId(), request.amount(), request.description(), idempotencyKey,
                CurrentOperator.idOf(jwt))));
    }

    @PostMapping("/transfers/{id}/reversal")
    public ResponseEntity<TransferResponse> reverse(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestHeader(IDEMPOTENCY_KEY) @Pattern(regexp = KEY_FORMAT) String idempotencyKey,
            @Valid @RequestBody ReversalRequest request) {
        return respond(transfers.submit(TransferCommand.reversal(id, request.reason(), idempotencyKey,
                CurrentOperator.idOf(jwt))));
    }

    @GetMapping("/transfers")
    public PageResponse<TransferResponse> search(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");

        return transfers.search(accountId, PageRequests.create(page, size, sort));
    }

    @GetMapping("/transfers/{id}")
    public TransferResponse find(@PathVariable UUID id) {
        return transfers.find(id);
    }

    /** 201 for a new transfer; 200 plus Idempotent-Replayed for a retry of one that already exists. */
    private static ResponseEntity<TransferResponse> respond(PostingResult result) {
        if (result.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(result.transfer());
        }
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + result.transfer().id())).body(result.transfer());
    }
}
