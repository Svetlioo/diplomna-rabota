package bg.tu_sofia.diploma.transaction.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByFromAccountIdOrToAccountId(UUID fromAccountId, UUID toAccountId, Pageable pageable);
}
