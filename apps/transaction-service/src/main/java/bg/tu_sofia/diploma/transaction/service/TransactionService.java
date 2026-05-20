package bg.tu_sofia.diploma.transaction.service;

import bg.tu_sofia.diploma.transaction.client.AccountClient;
import bg.tu_sofia.diploma.transaction.client.AccountClientException;
import bg.tu_sofia.diploma.transaction.domain.Transaction;
import bg.tu_sofia.diploma.transaction.domain.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    /**
     * Orchestrates a transfer between two accounts held by account-service:
     * withdraw from the source, then deposit into the target. If the deposit
     * fails after a successful withdrawal, the withdrawal is compensated
     * (refunded) so no money is lost — a lightweight saga. Every outcome,
     * success or failure, is persisted as a transaction record.
     */
    public Transaction transfer(UUID from, UUID to, BigDecimal amount, String currency) {
        if (from.equals(to)) {
            throw new SameAccountTransferException();
        }

        try {
            accountClient.withdraw(from, amount);
        } catch (AccountClientException e) {
            return transactionRepository.save(
                    Transaction.failed(from, to, amount, currency, "withdraw: " + e.getMessage()));
        }

        try {
            accountClient.deposit(to, amount);
        } catch (AccountClientException e) {
            compensateWithdrawal(from, amount);
            return transactionRepository.save(
                    Transaction.failed(from, to, amount, currency, "deposit: " + e.getMessage()));
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

    private void compensateWithdrawal(UUID from, BigDecimal amount) {
        try {
            accountClient.deposit(from, amount);
        } catch (AccountClientException e) {
            // Refund failed — flag loudly for manual reconciliation.
            log.error("Compensation failed: could not refund {} to account {} ({})", amount, from, e.getMessage());
        }
    }
}
