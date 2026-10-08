package dev.tutz.ledger.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
