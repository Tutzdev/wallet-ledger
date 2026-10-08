package dev.tutz.ledger.report;

import dev.tutz.ledger.account.AccountRepository;
import dev.tutz.ledger.account.AccountStatus;
import dev.tutz.ledger.account.AccountType;
import dev.tutz.ledger.outbox.OutboxRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side for the dashboard: plain SQL aggregates, no entities loaded. */
@Service
public class LedgerReportService {

    private final JdbcClient jdbc;
    private final AccountRepository accounts;
    private final OutboxRepository outbox;
    private final Clock clock;

    public LedgerReportService(JdbcClient jdbc, AccountRepository accounts, OutboxRepository outbox, Clock clock) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public IntegrityReport integrity() {
        List<CurrencyAmount> netByCurrency = jdbc.sql("""
                        SELECT currency, CAST(SUM(amount_minor) AS BIGINT) AS net
                        FROM ledger_entries
                        GROUP BY currency
                        ORDER BY currency
                        """)
                .query((row, rowNumber) -> CurrencyAmount.of(row.getString("currency"), row.getLong("net")))
                .list();
        long unbalancedTransfers = jdbc.sql("""
                        SELECT COUNT(*) FROM (
                            SELECT transfer_id FROM ledger_entries
                            GROUP BY transfer_id
                            HAVING SUM(amount_minor) <> 0
                        ) AS unbalanced
                        """)
                .query(Long.class)
                .single();
        boolean balanced = unbalancedTransfers == 0
                && netByCurrency.stream().allMatch(net -> net.amount().signum() == 0);

        return new IntegrityReport(balanced, unbalancedTransfers, netByCurrency);
    }

    @Transactional(readOnly = true)
    public LedgerSummary summary() {
        Instant now = clock.instant();
        Timestamp startOfDay = Timestamp.from(LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).toInstant());

        long customerAccounts = accounts.countByType(AccountType.CUSTOMER);
        long frozenAccounts = accounts.countByTypeAndStatus(AccountType.CUSTOMER, AccountStatus.FROZEN);
        long transfersToday = jdbc.sql("SELECT COUNT(*) FROM transfers WHERE created_at >= :since")
                .param("since", startOfDay)
                .query(Long.class)
                .single();
        List<CurrencyAmount> volumeToday = jdbc.sql("""
                        SELECT currency, CAST(SUM(amount_minor) AS BIGINT) AS total
                        FROM transfers
                        WHERE created_at >= :since
                        GROUP BY currency
                        ORDER BY currency
                        """)
                .param("since", startOfDay)
                .query((row, rowNumber) -> CurrencyAmount.of(row.getString("currency"), row.getLong("total")))
                .list();
        List<CurrencyAmount> heldByCustomers = jdbc.sql("""
                        SELECT e.currency, CAST(SUM(e.amount_minor) AS BIGINT) AS total
                        FROM ledger_entries e
                        JOIN accounts a ON a.id = e.account_id
                        WHERE a.type = 'CUSTOMER'
                        GROUP BY e.currency
                        ORDER BY e.currency
                        """)
                .query((row, rowNumber) -> CurrencyAmount.of(row.getString("currency"), row.getLong("total")))
                .list();
        Long oldestPendingSeconds = outbox.findOldestPendingCreatedAt()
                .map(createdAt -> Duration.between(createdAt, now).toSeconds())
                .orElse(null);

        return new LedgerSummary(customerAccounts, frozenAccounts, transfersToday, volumeToday, heldByCustomers,
                outbox.countByPublishedAtIsNull(), oldestPendingSeconds);
    }
}
