package dev.tutz.ledger.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, UUID> {

    Optional<Operator> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
