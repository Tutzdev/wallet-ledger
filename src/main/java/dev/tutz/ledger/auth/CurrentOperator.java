package dev.tutz.ledger.auth;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentOperator {

    private CurrentOperator() {
    }

    public static UUID idOf(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
