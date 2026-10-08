package dev.tutz.ledger.common;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid e-mail or password.");
    }
}
