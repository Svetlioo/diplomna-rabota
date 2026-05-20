package bg.tu_sofia.diploma.bank.service;

import bg.tu_sofia.diploma.bank.exception.SuspiciousTransferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Screens a transfer against the fraud-detection service BEFORE it is executed,
 * so a flagged transfer is blocked and the money never moves. fraud-detection is
 * stateless: it receives the transfer in the request and returns a verdict — it
 * never touches this service's database. A flagged transfer also freezes the
 * source account (this service is the only writer of its data), blocking further
 * activity. The fraud call is made outside any money-movement transaction, and
 * failures are swallowed (fail-open): a fraud outage must not break payments.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudScreeningService {

    private final RestClient fraudRestClient;
    private final AccountService accountService;

    public void check(UUID ownerId, String toIban, BigDecimal amount) {
        if (isSuspicious(ownerId, toIban, amount)) {
            accountService.freeze(ownerId);
            log.warn("Transfer from account owner {} blocked and account frozen by fraud-detection", ownerId);
            throw new SuspiciousTransferException();
        }
    }

    private boolean isSuspicious(UUID ownerId, String toIban, BigDecimal amount) {
        try {
            Verdict verdict = fraudRestClient.post()
                    .uri("/evaluate")
                    .body(new EvaluateRequest(ownerId, toIban, amount))
                    .retrieve()
                    .body(Verdict.class);
            return verdict != null && verdict.suspicious();
        } catch (Exception e) {
            log.warn("Fraud screening unavailable, allowing transfer: {}", e.getMessage());
            return false;
        }
    }

    private record EvaluateRequest(UUID ownerId, String toIban, BigDecimal amount) {
    }

    private record Verdict(boolean suspicious, String reason) {
    }
}
