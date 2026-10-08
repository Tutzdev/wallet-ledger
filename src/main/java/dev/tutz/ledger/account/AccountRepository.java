package dev.tutz.ledger.account;

import dev.tutz.ledger.money.CurrencyCode;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * {@code SELECT ... FOR UPDATE} in id order. Every transfer locks its accounts in the same order,
     * so two opposite transfers (A to B and B to A) can never deadlock each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllInIdOrder(Collection<UUID> ids);

    Optional<Account> findByTypeAndCurrency(AccountType type, CurrencyCode currency);

    Page<Account> findByType(AccountType type, Pageable pageable);

    long countByType(AccountType type);

    long countByTypeAndStatus(AccountType type, AccountStatus status);

    @Query(value = """
            SELECT CAST(COALESCE(SUM(amount_minor), 0) AS BIGINT)
            FROM ledger_entries
            WHERE account_id = :accountId
            """, nativeQuery = true)
    long balanceOf(UUID accountId);

    /** One query for a whole page of accounts instead of one per row. */
    @Query(value = """
            SELECT account_id AS accountId, CAST(SUM(amount_minor) AS BIGINT) AS balance
            FROM ledger_entries
            WHERE account_id IN (:accountIds)
            GROUP BY account_id
            """, nativeQuery = true)
    List<AccountBalance> balancesOf(Collection<UUID> accountIds);

    interface AccountBalance {

        UUID getAccountId();

        long getBalance();
    }
}
