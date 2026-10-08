package dev.tutz.ledger.auth;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Creates the first operator from LEDGER_ADMIN_EMAIL / LEDGER_ADMIN_PASSWORD, so no credential lives in the code. */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final OperatorRepository operators;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    AdminBootstrap(OperatorRepository operators, PasswordEncoder passwordEncoder, SecurityProperties properties,
            Clock clock) {
        this.operators = operators;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.adminEmail();
        String password = properties.adminPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.info("No admin credentials configured; skipping operator bootstrap.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("LEDGER_ADMIN_PASSWORD must have at least 12 characters.");
        }
        if (operators.existsByEmailIgnoreCase(email)) {
            return;
        }

        operators.save(new Operator(email, passwordEncoder.encode(password), clock.instant()));
        log.info("Admin operator created.");
    }
}
