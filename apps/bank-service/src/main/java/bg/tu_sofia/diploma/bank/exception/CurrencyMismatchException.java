package bg.tu_sofia.diploma.bank.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException() {
        super("Accounts and transfer must share the same currency");
    }
}
