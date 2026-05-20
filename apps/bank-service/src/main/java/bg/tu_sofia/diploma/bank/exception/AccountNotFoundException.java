package bg.tu_sofia.diploma.bank.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("Account not found: " + id);
    }

    private AccountNotFoundException(String message) {
        super(message);
    }

    public static AccountNotFoundException forOwner() {
        return new AccountNotFoundException("You do not have an account yet");
    }

    public static AccountNotFoundException forIban(String iban) {
        return new AccountNotFoundException("No account with IBAN " + iban);
    }
}
