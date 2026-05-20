package bg.tu_sofia.diploma.bank.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
