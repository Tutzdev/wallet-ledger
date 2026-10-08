package dev.tutz.ledger.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tutz.ledger.PostgresTestSupport;
import dev.tutz.ledger.account.AccountRepository;
import dev.tutz.ledger.common.BusinessRuleException;
import dev.tutz.ledger.report.LedgerReportService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TransferConcurrencyTests extends PostgresTestSupport {

    private static final BigDecimal TEN = new BigDecimal("10.00");

    @Autowired
    private TransferService transfers;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerReportService reports;

    @Test
    void concurrentTransfersNeverOverdrawTheSourceAccount() throws Exception {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        UUID bob = openAccount("Bob");
        transfers.submit(TransferCommand.deposit(alice, new BigDecimal("100.00"), "Initial funds", newKey(), operator));

        List<Boolean> outcomes = runConcurrently(50, () -> {
            try {
                transfers.submit(TransferCommand.transfer(alice, bob, TEN, null, newKey(), operator));
                return true;
            } catch (BusinessRuleException exception) {
                assertThat(exception.getCode()).isEqualTo("INSUFFICIENT_FUNDS");
                return false;
            }
        });

        assertThat(outcomes).filteredOn(Boolean::booleanValue).hasSize(10);
        assertThat(accountRepository.balanceOf(alice)).isZero();
        assertThat(accountRepository.balanceOf(bob)).isEqualTo(10_000);
        assertThat(reports.integrity().balanced()).isTrue();
    }

    @Test
    void oppositeTransfersBetweenTheSameAccountsDoNotDeadlock() throws Exception {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        UUID bob = openAccount("Bob");
        transfers.submit(TransferCommand.deposit(alice, new BigDecimal("500.00"), null, newKey(), operator));
        transfers.submit(TransferCommand.deposit(bob, new BigDecimal("500.00"), null, newKey(), operator));

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> transfer(alice, bob, operator));
            tasks.add(() -> transfer(bob, alice, operator));
        }
        List<Boolean> outcomes = runAll(tasks);

        assertThat(outcomes).containsOnly(true);
        assertThat(accountRepository.balanceOf(alice)).isEqualTo(50_000);
        assertThat(accountRepository.balanceOf(bob)).isEqualTo(50_000);
    }

    private boolean transfer(UUID from, UUID to, UUID operator) {
        transfers.submit(TransferCommand.transfer(from, to, TEN, null, newKey(), operator));
        return true;
    }

    private static List<Boolean> runConcurrently(int times, Callable<Boolean> task) throws Exception {
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            tasks.add(task);
        }
        return runAll(tasks);
    }

    /** Every task waits on the same latch, so they hit the database at the same moment. */
    private static List<Boolean> runAll(List<Callable<Boolean>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();

            List<Boolean> outcomes = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes;
        }
    }
}
