package bg.tu_sofia.diploma.account.service;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException() {
        super("This user already has an account");
    }
}
