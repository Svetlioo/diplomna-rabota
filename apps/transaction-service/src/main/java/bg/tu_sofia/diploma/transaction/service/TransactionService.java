package bg.tu_sofia.diploma.transaction.service;

import bg.tu_sofia.diploma.transaction.client.AccountClient;
import bg.tu_sofia.diploma.transaction.client.AccountClientException;
import bg.tu_sofia.diploma.transaction.domain.Transaction;
import bg.tu_sofia.diploma.transaction.domain.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String CURRENCY = "EUR";

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    /**
     * Records a transfer initiated by {@code ownerId} to {@code toIban}. The money
     * movement itself is one atomic operation inside account-service (/transfers),
     * which resolves the source from the relayed token and debits + credits in a
     * single locked database transaction. This service is the business entrypoint
     * and ledger: it asks account-service to perform the transfer and persists the
     * outcome — COMPLETED, or FAILED with the reason account-service reported.
     */
    public Transaction transfer(UUID ownerId, String toIban, BigDecimal amount) {
        try {
            accountClient.transfer(toIban, amount);
        } catch (AccountClientException e) {
            return transactionRepository.save(
                    Transaction.failed(ownerId, toIban, amount, CURRENCY, e.getMessage()));
        }
        return transactionRepository.save(Transaction.completed(ownerId, toIban, amount, CURRENCY));
    }

    @Transactional(readOnly = true)
    public Transaction getOwnTransaction(UUID id, UUID ownerId) {
        return transactionRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getOwnTransactions(UUID ownerId, Pageable pageable) {
        return transactionRepository.findByOwnerId(ownerId, pageable);
    }
}
