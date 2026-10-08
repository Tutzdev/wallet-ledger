package dev.tutz.ledger.common;

/**
 * A request that is well formed but breaks a ledger rule (insufficient funds, frozen account...).
 * The {@code code} is stable, so API clients can branch on it without parsing the message.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
