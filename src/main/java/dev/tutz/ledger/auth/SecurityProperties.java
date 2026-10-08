package dev.tutz.ledger.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Secrets come from the environment. The app refuses to start with a weak JWT secret. */
@Validated
@ConfigurationProperties("ledger.security")
public record SecurityProperties(
        @NotNull @Size(min = 32, message = "LEDGER_JWT_SECRET must have at least 32 characters") String jwtSecret,
        @NotNull Duration tokenTtl,
        String adminEmail,
        String adminPassword) {
}
