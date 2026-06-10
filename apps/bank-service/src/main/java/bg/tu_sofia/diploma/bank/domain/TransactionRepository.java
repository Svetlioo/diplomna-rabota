package bg.tu_sofia.diploma.bank.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByOwnerId(UUID ownerId, Pageable pageable);

    // Sent (ownerId) + received (toIban).
    Page<Transaction> findByOwnerIdOrToIban(UUID ownerId, String toIban, Pageable pageable);

    Optional<Transaction> findByIdAndOwnerId(UUID id, UUID ownerId);
}
