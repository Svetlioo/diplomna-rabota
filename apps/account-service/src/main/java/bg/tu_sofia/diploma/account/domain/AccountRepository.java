package bg.tu_sofia.diploma.account.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByIban(String iban);

    Optional<Account> findByOwnerId(UUID ownerId);

    Optional<Account> findByIban(String iban);

    /**
     * Loads an account by id with a row-level write lock (SELECT ... FOR UPDATE),
     * blocking any concurrent transaction from reading-for-update or writing the
     * same row until this transaction commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}
