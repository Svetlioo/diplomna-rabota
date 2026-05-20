package bg.tu_sofia.diploma.transaction.client;

/**
 * Raised when a call to account-service fails. The message is a human-readable
 * reason recorded on the failed transaction (e.g. "insufficient funds").
 */
public class AccountClientException extends RuntimeException {

    public AccountClientException(String reason) {
        super(reason);
    }
}
