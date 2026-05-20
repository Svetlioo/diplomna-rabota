package bg.tu_sofia.diploma.bank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ledger entry for a completed transfer. Only successful transfers are recorded
 * (failures surface as HTTP errors), so there is no status field. The row is
 * written in the same transaction as the money movement, so it can never exist
 * without the money having actually moved.
 */
@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "to_iban", nullable = false, updatable = false)
    private String toIban;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Transaction record(UUID ownerId, String toIban, BigDecimal amount, String currency) {
        Transaction tx = new Transaction();
        tx.id = UUID.randomUUID();
        tx.ownerId = ownerId;
        tx.toIban = toIban;
        tx.amount = amount;
        tx.currency = currency;
        return tx;
    }
}
