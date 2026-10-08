package dev.tutz.ledger.account;

import dev.tutz.ledger.common.PageRequests;
import dev.tutz.ledger.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accounts.open(request);

        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<AccountResponse> search(
            @RequestParam(defaultValue = "CUSTOMER") AccountType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");

        return accounts.search(type, PageRequests.create(page, size, sort));
    }

    @GetMapping("/{id}")
    public AccountResponse find(@PathVariable UUID id) {
        return accounts.find(id);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeAccountStatusRequest request) {
        return accounts.changeStatus(id, request.status());
    }
}
