package bg.tu_sofia.diploma.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Account(
        UUID id,
        String ownerName,
        BigDecimal balance,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
