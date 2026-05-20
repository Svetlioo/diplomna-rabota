package bg.tu_sofia.diploma.bank.service;

import bg.tu_sofia.diploma.bank.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use-case orchestration for making a payment: screen first, then execute. It is
 * deliberately NOT transactional — the fraud call must stay outside the money
 * movement, and a flagged transfer is rejected before any transaction is opened.
 * The actual debit + credit + ledger insert happen atomically inside
 * {@link TransactionService#transfer}.
 */
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
