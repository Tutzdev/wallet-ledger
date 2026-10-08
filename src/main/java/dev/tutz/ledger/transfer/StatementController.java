package dev.tutz.ledger.transfer;

import dev.tutz.ledger.account.AccountService;
import dev.tutz.ledger.common.PageRequests;
import dev.tutz.ledger.common.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatementController {

    private final AccountService accounts;
    private final LedgerEntryRepository entries;

    public StatementController(AccountService accounts, LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    @GetMapping("/api/v1/accounts/{id}/entries")
    @Transactional(readOnly = true)
    public PageResponse<StatementLineResponse> statement(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        accounts.require(id);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");

        return PageResponse.from(entries.findStatement(id, PageRequests.create(page, size, sort))
                .map(StatementLineResponse::from));
    }
}
