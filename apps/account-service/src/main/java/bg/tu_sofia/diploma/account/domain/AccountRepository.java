package bg.tu_sofia.diploma.account.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private static final String COLUMNS =
            "id, owner_name, balance, version, created_at, updated_at";

    private final JdbcClient jdbcClient;

    public Account insert(UUID id, String ownerName, BigDecimal initialBalance) {
        return jdbcClient.sql("""
                        INSERT INTO accounts (id, owner_name, balance, version, created_at, updated_at)
                        VALUES (:id, :ownerName, :initialBalance, 0, NOW(), NOW())
                        RETURNING %s
                        """.formatted(COLUMNS))
                .param("id", id)
                .param("ownerName", ownerName)
                .param("initialBalance", initialBalance)
                .query(Account.class)
                .single();
    }

    public Optional<Account> findById(UUID id) {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM accounts WHERE id = :id")
                .param("id", id)
                .query(Account.class)
                .optional();
    }

    public Account deposit(UUID id, BigDecimal amount) {
        return jdbcClient.sql("""
                        UPDATE accounts
                        SET balance    = balance + :amount,
                            version    = version + 1,
                            updated_at = NOW()
                        WHERE id = :id
                        RETURNING %s
                        """.formatted(COLUMNS))
                .param("amount", amount)
                .param("id", id)
                .query(Account.class)
                .single();
    }

    public Optional<Account> withdrawIfSufficient(UUID id, BigDecimal amount) {
        return jdbcClient.sql("""
                        UPDATE accounts
                        SET balance    = balance - :amount,
                            version    = version + 1,
                            updated_at = NOW()
                        WHERE id = :id AND balance >= :amount
                        RETURNING %s
                        """.formatted(COLUMNS))
                .param("amount", amount)
                .param("id", id)
                .query(Account.class)
                .optional();
    }
}
