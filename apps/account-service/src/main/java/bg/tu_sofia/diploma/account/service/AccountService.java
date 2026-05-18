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
    public Account createAccount(String ownerName, BigDecimal initialBalance, String currency) {
        Account account = Account.open(ownerName, initialBalance, currency);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account deposit(UUID id, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.deposit(amount);
        return accountRepository.save(account);
    }

    @Transactional
    public Account withdraw(UUID id, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        try {
            account.withdraw(amount);
        } catch (IllegalStateException e) {
            throw new InsufficientFundsException();
        }
        return accountRepository.save(account);
    }
}
