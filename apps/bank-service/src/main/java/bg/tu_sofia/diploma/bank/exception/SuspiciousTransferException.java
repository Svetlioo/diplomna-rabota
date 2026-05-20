package bg.tu_sofia.diploma.bank.exception;

public class SuspiciousTransferException extends RuntimeException {

    public SuspiciousTransferException() {
        super("Transfer flagged as suspicious");
    }
}
