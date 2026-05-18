package bg.tu_sofia.diploma.account.web.dto;

import bg.tu_sofia.diploma.account.domain.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String ownerName,
        BigDecimal balance,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerName(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
