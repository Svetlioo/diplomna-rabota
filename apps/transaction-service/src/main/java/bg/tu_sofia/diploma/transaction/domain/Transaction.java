package bg.tu_sofia.diploma.transaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Transaction(UUID ownerId, String toIban, BigDecimal amount, String currency) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.toIban = toIban;
        this.amount = amount;
        this.currency = currency;
    }

    public static Transaction completed(UUID ownerId, String toIban, BigDecimal amount, String currency) {
        Transaction tx = new Transaction(ownerId, toIban, amount, currency);
        tx.status = TransactionStatus.COMPLETED;
        return tx;
    }

    public static Transaction failed(UUID ownerId, String toIban, BigDecimal amount, String currency, String reason) {
        Transaction tx = new Transaction(ownerId, toIban, amount, currency);
        tx.status = TransactionStatus.FAILED;
        tx.failureReason = reason;
        return tx;
    }
}
