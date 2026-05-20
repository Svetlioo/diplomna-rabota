package bg.tu_sofia.diploma.bank.service;

import bg.tu_sofia.diploma.bank.domain.Account;
import bg.tu_sofia.diploma.bank.domain.AccountRepository;
import bg.tu_sofia.diploma.bank.exception.AccountNotFoundException;
import bg.tu_sofia.diploma.bank.exception.CurrencyMismatchException;
import bg.tu_sofia.diploma.bank.exception.InsufficientFundsException;
import bg.tu_sofia.diploma.bank.exception.SameAccountTransferException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final IbanGenerator ibanGenerator;

    /**
     * Opens a user's single account at registration time. The owner is the newly
     * registered user, so a user always has exactly one account; the one-per-user
     * invariant is guaranteed by the UNIQUE constraint on accounts.owner_id.
     */
    @Transactional
    public Account openForOwner(UUID ownerId) {
        return accountRepository.save(Account.open(ownerId, uniqueIban()));
    }

    @Transactional(readOnly = true)
    public Account getOwnAccount(UUID ownerId) {
        return accountRepository.findByOwnerId(ownerId)
                .orElseThrow(AccountNotFoundException::forOwner);
    }

    @Transactional
    public Account deposit(UUID ownerId, BigDecimal amount) {
        Account account = lockOwnAccount(ownerId);
        account.deposit(amount);
        return accountRepository.save(account);
    }

    @Transactional
    public Account withdraw(UUID ownerId, BigDecimal amount) {
        Account account = lockOwnAccount(ownerId);
        try {
            account.withdraw(amount);
        } catch (IllegalStateException e) {
            throw new InsufficientFundsException();
        }
        return accountRepository.save(account);
    }

    /**
     * Moves money from the caller's account to the account with {@code toIban}.
     * The source is always the caller's own account (resolved from the
     * authenticated identity), so a user can never debit someone else's account.
     * Both rows are locked with SELECT ... FOR UPDATE in id order and the debit +
     * credit happen in one transaction, so either both sides change or neither
     * does, and two concurrent transfers over the same pair cannot deadlock.
     */
    @Transactional
    public void transfer(UUID ownerId, String toIban, BigDecimal amount) {
        UUID fromId = accountRepository.findByOwnerId(ownerId)
                .orElseThrow(AccountNotFoundException::forOwner)
                .getId();
        UUID toId = accountRepository.findByIban(toIban)
                .orElseThrow(() -> AccountNotFoundException.forIban(toIban))
                .getId();
        if (fromId.equals(toId)) {
            throw new SameAccountTransferException();
        }

        boolean fromFirst = fromId.compareTo(toId) < 0;
        UUID firstId = fromFirst ? fromId : toId;
        UUID secondId = fromFirst ? toId : fromId;
        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account from = fromFirst ? first : second;
        Account to = fromFirst ? second : first;

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new CurrencyMismatchException();
        }

        try {
            from.withdraw(amount);
        } catch (IllegalStateException e) {
            throw new InsufficientFundsException();
        }
        to.deposit(amount);

        accountRepository.save(from);
        accountRepository.save(to);
    }

    private Account lockOwnAccount(UUID ownerId) {
        UUID id = accountRepository.findByOwnerId(ownerId)
                .orElseThrow(AccountNotFoundException::forOwner)
                .getId();
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(AccountNotFoundException::forOwner);
    }

    private String uniqueIban() {
        String iban;
        do {
            iban = ibanGenerator.generate();
        } while (accountRepository.existsByIban(iban));
        return iban;
    }
}
