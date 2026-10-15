package dev.tutz.ledger.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.tutz.ledger.PostgresTestSupport;
import dev.tutz.ledger.account.AccountRepository;
import dev.tutz.ledger.account.AccountStatus;
import dev.tutz.ledger.common.BusinessRuleException;
import dev.tutz.ledger.outbox.OutboxEvent;
import dev.tutz.ledger.outbox.OutboxRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TransferServiceTests extends PostgresTestSupport {

    @Autowired
    private TransferService transfers;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository entries;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxRepository outbox;

    @Autowired
    private ObjectMapper json;

    @Test
    void retryWithTheSameKeyReturnsTheOriginalTransfer() {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        String key = newKey();

        PostingResult first = transfers.submit(TransferCommand.deposit(alice, new BigDecimal("25.00"), null, key, operator));
        PostingResult retry = transfers.submit(TransferCommand.deposit(alice, new BigDecimal("25.0"), null, key, operator));

        assertThat(first.replayed()).isFalse();
        assertThat(retry.replayed()).isTrue();
        assertThat(retry.transfer().id()).isEqualTo(first.transfer().id());
        assertThat(entries.findByTransferId(first.transfer().id())).hasSize(2);
        assertThat(accountRepository.balanceOf(alice)).isEqualTo(2_500);
    }

    @Test
    void sameKeyWithADifferentRequestIsRejected() {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        String key = newKey();
        transfers.submit(TransferCommand.deposit(alice, new BigDecimal("25.00"), null, key, operator));

        assertThatThrownBy(() -> transfers.submit(TransferCommand.deposit(alice, new BigDecimal("99.00"), null, key, operator)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    void concurrentRetriesWithTheSameKeyMoveTheMoneyOnce() throws Exception {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        String key = newKey();
        CountDownLatch start = new CountDownLatch(1);

        List<UUID> transferIds = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PostingResult>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return transfers.submit(TransferCommand.deposit(alice, new BigDecimal("10.00"), null, key, operator));
                }));
            }
            start.countDown();
            for (Future<PostingResult> future : futures) {
                transferIds.add(future.get().transfer().id());
            }
        }

        assertThat(transferIds).containsOnly(transferIds.getFirst());
        assertThat(accountRepository.balanceOf(alice)).isEqualTo(1_000);
    }

    @Test
    void aTransferCanBeReversedOnlyOnce() {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        UUID bob = openAccount("Bob");
        transfers.submit(TransferCommand.deposit(alice, new BigDecimal("50.00"), null, newKey(), operator));
        UUID transferId = transfers.submit(TransferCommand.transfer(alice, bob, new BigDecimal("20.00"), null, newKey(), operator))
                .transfer().id();

        transfers.submit(TransferCommand.reversal(transferId, "Sent by mistake", newKey(), operator));

        assertThat(accountRepository.balanceOf(alice)).isEqualTo(5_000);
        assertThat(accountRepository.balanceOf(bob)).isZero();
        assertThat(transferRepository.existsByReversalOfId(transferId)).isTrue();
        assertThatThrownBy(() -> transfers.submit(TransferCommand.reversal(transferId, "Again", newKey(), operator)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("ALREADY_REVERSED");
    }

    @Test
    void aFrozenAccountCanReceiveButCannotSend() {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        UUID bob = openAccount("Bob");
        transfers.submit(TransferCommand.deposit(alice, new BigDecimal("30.00"), null, newKey(), operator));
        accounts.changeStatus(alice, AccountStatus.FROZEN);

        transfers.submit(TransferCommand.deposit(alice, new BigDecimal("5.00"), null, newKey(), operator));

        assertThatThrownBy(() -> transfers.submit(TransferCommand.transfer(alice, bob, new BigDecimal("1.00"), null, newKey(), operator)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("ACCOUNT_FROZEN");
        assertThat(accountRepository.balanceOf(alice)).isEqualTo(3_500);
    }

    @Test
    void thePostedEventCarriesTheRequestIdempotencyKey() {
        UUID operator = newOperator();
        UUID alice = openAccount("Alice");
        UUID bob = openAccount("Bob");
        transfers.submit(TransferCommand.deposit(alice, new BigDecimal("50.00"), null, newKey(), operator));

        UUID transferId = transfers.submit(TransferCommand.transfer(alice, bob, new BigDecimal("20.00"), "Booking 42",
                "booking-42", operator)).transfer().id();

        OutboxEvent event = outbox.findAll().stream()
                .filter(candidate -> candidate.getAggregateId().equals(transferId))
                .findFirst().orElseThrow();
        JsonNode payload = json.readTree(event.getPayload());
        assertThat(event.getEventType()).isEqualTo("transfer.posted");
        assertThat(payload.get("transferId").asString()).isEqualTo(transferId.toString());
        assertThat(payload.get("idempotencyKey").asString()).isEqualTo("booking-42");
    }
}
