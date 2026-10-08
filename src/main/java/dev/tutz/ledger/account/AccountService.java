package dev.tutz.ledger.account;

import dev.tutz.ledger.account.AccountRepository.AccountBalance;
import dev.tutz.ledger.common.PageResponse;
import dev.tutz.ledger.common.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final Clock clock;

    public AccountService(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse open(CreateAccountRequest request) {
        Account account = accounts.save(Account.openCustomerAccount(request.ownerName(), request.currency(), clock.instant()));

        return AccountResponse.from(account, 0L);
    }

    @Transactional(readOnly = true)
    public AccountResponse find(UUID accountId) {
        Account account = require(accountId);

        return AccountResponse.from(account, accounts.balanceOf(accountId));
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> search(AccountType type, Pageable pageable) {
        Page<Account> page = accounts.findByType(type, pageable);
        Map<UUID, Long> balances = balancesOf(page.map(Account::getId).toList());

        return PageResponse.from(page.map(account ->
                AccountResponse.from(account, balances.getOrDefault(account.getId(), 0L))));
    }

    @Transactional
    public AccountResponse changeStatus(UUID accountId, AccountStatus status) {
        Account account = require(accountId);
        account.changeStatus(status, clock.instant());

        return AccountResponse.from(account, accounts.balanceOf(accountId));
    }

    @Transactional(readOnly = true)
    public Account require(UUID accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account %s not found.".formatted(accountId)));
    }

    private Map<UUID, Long> balancesOf(List<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accounts.balancesOf(accountIds).stream()
                .collect(Collectors.toUnmodifiableMap(AccountBalance::getAccountId, AccountBalance::getBalance));
    }
}
