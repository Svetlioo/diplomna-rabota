package bg.tu_sofia.diploma.bank.exception;

public class FraudScreeningUnavailableException extends RuntimeException {

    public FraudScreeningUnavailableException() {
        super("Fraud screening is unavailable, transfer blocked");
    }
}
