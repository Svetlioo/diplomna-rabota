package bg.tu_sofia.diploma.transaction.web.dto;

import bg.tu_sofia.diploma.transaction.domain.Transaction;
import bg.tu_sofia.diploma.transaction.domain.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String failureReason,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getFromAccountId(),
                tx.getToAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getFailureReason(),
                tx.getCreatedAt()
        );
    }
}
