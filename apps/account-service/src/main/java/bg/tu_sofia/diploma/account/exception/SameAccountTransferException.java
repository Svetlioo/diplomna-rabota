package bg.tu_sofia.diploma.account.exception;

public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("Source and target accounts must differ");
    }
}
