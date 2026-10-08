package dev.tutz.ledger;

import dev.tutz.ledger.account.AccountService;
import dev.tutz.ledger.account.CreateAccountRequest;
import dev.tutz.ledger.auth.Operator;
import dev.tutz.ledger.auth.OperatorRepository;
import dev.tutz.ledger.money.CurrencyCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs against a real PostgreSQL: row locks, the append-only triggers and SKIP LOCKED
 * cannot be faked by an in-memory database. One container is shared by every test class.
 */
@SpringBootTest(properties = {
        "ledger.security.jwt-secret=test-secret-with-at-least-32-characters",
        "ledger.outbox.poll-interval-ms=600000"
})
public abstract class PostgresTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected AccountService accounts;

    @Autowired
    private OperatorRepository operators;

    protected UUID newOperator() {
        String email = "operator-" + UUID.randomUUID() + "@ledger.test";
        return operators.save(new Operator(email, "not-used-in-tests", Instant.now())).getId();
    }

    protected UUID openAccount(String owner) {
        return accounts.open(new CreateAccountRequest(owner, CurrencyCode.BRL)).id();
    }

    protected static String newKey() {
        return UUID.randomUUID().toString();
    }
}
