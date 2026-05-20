package bg.tu_sofia.diploma.bank.web.dto;

import bg.tu_sofia.diploma.bank.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID ownerId,
        String toIban,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getOwnerId(),
                tx.getToIban(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getCreatedAt()
        );
    }
}
