package bg.tu_sofia.diploma.transaction.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thin REST client over account-service. The whole money movement is delegated
 * to account-service's atomic /transfers endpoint; on failure the reason
 * account-service reported (e.g. "Insufficient funds") is surfaced as an
 * {@link AccountClientException} so it can be recorded on the transaction.
 */
@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    public void transfer(UUID from, UUID to, BigDecimal amount, String currency) {
        try {
            restClient.post()
                    .uri("/transfers")
                    .body(new TransferRequest(from, to, amount, currency))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new AccountClientException(reasonFrom(e));
        }
    }

    private String reasonFrom(RestClientResponseException e) {
        try {
            ErrorBody body = e.getResponseBodyAs(ErrorBody.class);
            if (body != null && body.message() != null && !body.message().isBlank()) {
                return body.message();
            }
        } catch (Exception ignored) {
            // Fall through to a generic, status-based reason.
        }
        return "account-service error (" + e.getStatusCode().value() + ")";
    }

    private record TransferRequest(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
    }

    private record ErrorBody(String error, String message) {
    }
}
