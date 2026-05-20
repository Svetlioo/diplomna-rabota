package bg.tu_sofia.diploma.transaction.service;

import bg.tu_sofia.diploma.transaction.client.AccountClient;
import bg.tu_sofia.diploma.transaction.client.AccountClientException;
import bg.tu_sofia.diploma.transaction.domain.Transaction;
import bg.tu_sofia.diploma.transaction.domain.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    /**
     * Records a transfer between two accounts. The money movement itself is one
     * atomic operation inside account-service (/transfers), which debits the
     * source and credits the target in a single locked database transaction.
     * This service is the business entrypoint and ledger: it asks account-service
     * to perform the transfer and persists the outcome — COMPLETED, or FAILED
     * with the reason account-service reported (e.g. insufficient funds). Because
     * the transfer is atomic on account-service's side, it either fully happened
     * or not at all, so there is no compensation to run here.
     */
    public Transaction transfer(UUID from, UUID to, BigDecimal amount, String currency) {
        if (from.equals(to)) {
            throw new SameAccountTransferException();
        }

        try {
            accountClient.transfer(from, to, amount, currency);
        } catch (AccountClientException e) {
            return transactionRepository.save(
                    Transaction.failed(from, to, amount, currency, e.getMessage()));
        }

        return transactionRepository.save(Transaction.completed(from, to, amount, currency));
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getByAccount(UUID accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }
}
