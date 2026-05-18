package bg.tu_sofia.diploma.account.service;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("Account not found: " + id);
    }
}
