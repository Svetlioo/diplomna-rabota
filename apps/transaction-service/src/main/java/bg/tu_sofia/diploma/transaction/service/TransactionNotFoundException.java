package bg.tu_sofia.diploma.transaction.service;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID id) {
        super("Transaction not found: " + id);
    }
}
