package bg.tu_sofia.diploma.bank.service;

import bg.tu_sofia.diploma.bank.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

// Screen first, then transfer. Not @Transactional: the fraud call stays outside the money move.
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final FraudScreeningService fraudScreeningService;
    private final TransactionService transactionService;

    public Transaction pay(UUID ownerId, String toIban, BigDecimal amount) {
        fraudScreeningService.check(ownerId, toIban, amount);
        return transactionService.transfer(ownerId, toIban, amount);
    }
}
