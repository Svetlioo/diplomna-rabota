package bg.tu_sofia.diploma.bank.service;

import bg.tu_sofia.diploma.bank.domain.Transaction;
import bg.tu_sofia.diploma.bank.domain.TransactionRepository;
import bg.tu_sofia.diploma.bank.exception.TransactionNotFoundException;
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

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    // Money move + ledger insert in one transaction (both or neither).
    @Transactional
    public Transaction transfer(UUID ownerId, String toIban, BigDecimal amount) {
        accountService.transfer(ownerId, toIban, amount);
        return transactionRepository.save(Transaction.record(ownerId, toIban, amount, CURRENCY));
    }

    @Transactional(readOnly = true)
    public Transaction getOwnTransaction(UUID id, UUID ownerId) {
        return transactionRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getOwnTransactions(UUID ownerId, Pageable pageable) {
        String iban = accountService.getOwnAccount(ownerId).getIban();
        return transactionRepository.findByOwnerIdOrToIban(ownerId, iban, pageable);
    }
}
