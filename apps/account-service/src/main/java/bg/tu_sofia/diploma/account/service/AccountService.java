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

    /**
     * Moves money between two accounts atomically. Both rows are locked with
     * SELECT ... FOR UPDATE and the debit + credit happen in one transaction, so
     * either both sides change or neither does — there is no in-flight state and
     * no compensation to run. Locks are always acquired in id order, so two
     * concurrent transfers over the same pair (A→B and B→A) can never deadlock by
     * grabbing the rows in opposite order.
     */
    @Transactional
    public void transfer(UUID fromId, UUID toId, BigDecimal amount, String currency) {
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

        if (!from.getCurrency().equals(currency) || !to.getCurrency().equals(currency)) {
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
}
