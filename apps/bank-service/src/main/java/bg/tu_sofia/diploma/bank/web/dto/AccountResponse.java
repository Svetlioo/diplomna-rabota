package bg.tu_sofia.diploma.bank.web.dto;

import bg.tu_sofia.diploma.bank.domain.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID ownerId,
        String iban,
        BigDecimal balance,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerId(),
                account.getIban(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
