package dev.tutz.ledger.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A back-office user allowed to move money through the API and the dashboard. */
@Entity
@Table(name = "operators")
public class Operator {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt;

    protected Operator() {
    }

    public Operator(String email, String passwordHash, Instant now) {
        this.id = UUID.randomUUID();
        this.email = email.strip().toLowerCase();
        this.passwordHash = passwordHash;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }
}
