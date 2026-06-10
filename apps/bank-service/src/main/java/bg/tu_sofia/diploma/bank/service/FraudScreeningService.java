package bg.tu_sofia.diploma.bank.service;

import bg.tu_sofia.diploma.bank.exception.FraudScreeningUnavailableException;
import bg.tu_sofia.diploma.bank.exception.SuspiciousTransferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

// Screens a transfer before it executes; freezes the account if flagged, blocks if screening is unavailable.
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudScreeningService {

    private final RestClient fraudRestClient;
    private final AccountService accountService;

    public void check(UUID ownerId, String toIban, BigDecimal amount) {
        if (evaluate(ownerId, toIban, amount).suspicious()) {
            accountService.freeze(ownerId);
            log.warn("Transfer from account owner {} blocked and account frozen by fraud-detection", ownerId);
            throw new SuspiciousTransferException();
        }
    }

    private Verdict evaluate(UUID ownerId, String toIban, BigDecimal amount) {
        Verdict verdict;
        try {
            verdict = fraudRestClient.post()
                    .uri("/evaluate")
                    .body(new EvaluateRequest(ownerId, toIban, amount))
                    .retrieve()
                    .body(Verdict.class);
        } catch (Exception e) {
            log.warn("Fraud screening unavailable, blocking transfer: {}", e.getMessage());
            throw new FraudScreeningUnavailableException();
        }
        if (verdict == null) {
            throw new FraudScreeningUnavailableException();
        }
        return verdict;
    }

    private record EvaluateRequest(UUID ownerId, String toIban, BigDecimal amount) {
    }

    private record Verdict(boolean suspicious, String reason) {
    }
}
