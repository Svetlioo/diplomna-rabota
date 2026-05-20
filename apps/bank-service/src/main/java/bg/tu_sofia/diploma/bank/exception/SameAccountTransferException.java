package bg.tu_sofia.diploma.bank.exception;

public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("Source and target accounts must differ");
    }
}
