package bg.tu_sofia.diploma.account.service;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException() {
        super("Accounts and transfer must share the same currency");
    }
}
