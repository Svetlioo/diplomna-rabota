package bg.tu_sofia.diploma.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Screens a completed transfer against the fraud-detection service. Called after
 * the transfer has committed, so screening never sits inside the money-movement
 * transaction. fraud-detection is stateless: it receives the transfer in the
 * request and returns a verdict — it never touches this service's database. If a
 * transfer is flagged, the source account is frozen here (this service is the
 * only writer of its data). Failures are swallowed (fail-open): a fraud outage
 * must not break payments.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudScreeningService {

    private final RestClient fraudRestClient;
    private final AccountService accountService;

    public void screen(UUID ownerId, String toIban, BigDecimal amount) {
        try {
            Verdict verdict = fraudRestClient.post()
                    .uri("/evaluate")
                    .body(new EvaluateRequest(ownerId, toIban, amount))
                    .retrieve()
                    .body(Verdict.class);
            if (verdict != null && verdict.suspicious()) {
                accountService.freeze(ownerId);
                log.warn("Account {} frozen by fraud-detection: {}", ownerId, verdict.reason());
            }
        } catch (Exception e) {
            log.warn("Fraud screening unavailable, allowing transfer: {}", e.getMessage());
        }
    }

    private record EvaluateRequest(UUID ownerId, String toIban, BigDecimal amount) {
    }

    private record Verdict(boolean suspicious, String reason) {
    }
}
