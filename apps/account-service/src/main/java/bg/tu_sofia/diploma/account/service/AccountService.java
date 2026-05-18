package bg.tu_sofia.diploma.account.service;

import bg.tu_sofia.diploma.account.domain.Account;
import bg.tu_sofia.diploma.account.domain.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(String ownerName, BigDecimal initialBalance) {
        return accountRepository.insert(UUID.randomUUID(), ownerName, initialBalance);
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account deposit(UUID id, BigDecimal amount) {
        accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return accountRepository.deposit(id, amount);
    }

    @Transactional
    public Account withdraw(UUID id, BigDecimal amount) {
        accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return accountRepository.withdrawIfSufficient(id, amount)
                .orElseThrow(InsufficientFundsException::new);
    }
}
