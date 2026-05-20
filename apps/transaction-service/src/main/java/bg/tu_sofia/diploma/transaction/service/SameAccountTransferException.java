package bg.tu_sofia.diploma.transaction.service;

public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("Source and target accounts must differ");
    }
}
