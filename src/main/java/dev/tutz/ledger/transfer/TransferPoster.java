package dev.tutz.ledger.transfer;

import dev.tutz.ledger.account.Account;
import dev.tutz.ledger.account.AccountRepository;
import dev.tutz.ledger.account.AccountType;
import dev.tutz.ledger.common.BusinessRuleException;
import dev.tutz.ledger.common.ResourceNotFoundException;
import dev.tutz.ledger.money.Money;
import dev.tutz.ledger.outbox.OutboxWriter;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one transfer atomically: both accounts locked, rules checked against the locked state,
 * then the transfer, its two entries and its outbox event committed together.
 */
@Component
class TransferPoster {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository entries;
    private final OutboxWriter outbox;
    private final Clock clock;

    TransferPoster(AccountRepository accounts, TransferRepository transfers, LedgerEntryRepository entries,
            OutboxWriter outbox, Clock clock) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.entries = entries;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    Transfer post(TransferCommand command) {
        Route route = routeFor(command);
        List<Account> locked = accounts.lockAllInIdOrder(List.of(route.sourceId(), route.destinationId()));
        Account source = pick(locked, route.sourceId());
        Account destination = pick(locked, route.destinationId());
        Money amount = route.fixedAmount() != null ? route.fixedAmount() : Money.of(command.amount(), source.getCurrency());

        requireAllowed(command, source, destination, amount);

        Transfer transfer = transfers.save(new Transfer(command, source, destination, amount, clock.instant()));
        entries.saveAll(List.of(LedgerEntry.debit(transfer), LedgerEntry.credit(transfer)));
        outbox.append(transfer.getType().eventType(), transfer.getId(), TransferPostedEvent.from(transfer));

        return transfer;
    }

    private Route routeFor(TransferCommand command) {
        return switch (command.type()) {
            case DEPOSIT -> {
                Account destination = require(command.destinationAccountId());
                yield new Route(systemAccountOf(destination), destination.getId(), null);
            }
            case WITHDRAWAL -> {
                Account source = require(command.sourceAccountId());
                yield new Route(source.getId(), systemAccountOf(source), null);
            }
            case TRANSFER -> {
                if (command.sourceAccountId().equals(command.destinationAccountId())) {
                    throw new BusinessRuleException("SAME_ACCOUNT", "Source and destination must be different accounts.");
                }
                yield new Route(command.sourceAccountId(), command.destinationAccountId(), null);
            }
            case REVERSAL -> reversalRoute(command.reversalOfId());
        };
    }

    private Route reversalRoute(UUID originalId) {
        Transfer original = transfers.findById(originalId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer %s not found.".formatted(originalId)));
        if (original.getType() == TransferType.REVERSAL) {
            throw new BusinessRuleException("NOT_REVERSIBLE", "A reversal cannot be reversed.");
        }
        if (transfers.existsByReversalOfId(originalId)) {
            throw new BusinessRuleException("ALREADY_REVERSED", "Transfer %s was already reversed.".formatted(originalId));
        }

        return new Route(original.getDestinationAccountId(), original.getSourceAccountId(), original.amount());
    }

    /** The rules are checked only after the locks are held, so no concurrent transfer can change the answer. */
    private void requireAllowed(TransferCommand command, Account source, Account destination, Money amount) {
        if (source.getCurrency() != destination.getCurrency()) {
            throw new BusinessRuleException("CURRENCY_MISMATCH", "Both accounts must use the same currency.");
        }
        // Reversals are back-office corrections, so they still run on a frozen account.
        if (source.isFrozen() && command.type() != TransferType.REVERSAL) {
            throw new BusinessRuleException("ACCOUNT_FROZEN", "Account %s is frozen.".formatted(source.getId()));
        }
        if (!source.canGoNegative() && accounts.balanceOf(source.getId()) < amount.minorUnits()) {
            throw new BusinessRuleException("INSUFFICIENT_FUNDS",
                    "Account %s has insufficient funds.".formatted(source.getId()));
        }
    }

    private Account require(UUID accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account %s not found.".formatted(accountId)));
    }

    private UUID systemAccountOf(Account customer) {
        return accounts.findByTypeAndCurrency(AccountType.SYSTEM, customer.getCurrency())
                .map(Account::getId)
                .orElseThrow(() -> new IllegalStateException("Missing system account for " + customer.getCurrency()));
    }

    private static Account pick(List<Account> accounts, UUID accountId) {
        return accounts.stream()
                .filter(account -> account.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Account %s not found.".formatted(accountId)));
    }

    private record Route(UUID sourceId, UUID destinationId, Money fixedAmount) {
    }
}
