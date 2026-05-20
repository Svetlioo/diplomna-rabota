package bg.tu_sofia.diploma.bank.exception;

public class AccountFrozenException extends RuntimeException {

    public AccountFrozenException() {
        super("Account is frozen");
    }
}
