package bg.tu_sofia.diploma.transaction.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thin REST client over account-service. Translates account-service HTTP error
 * statuses into a single {@link AccountClientException} carrying a readable
 * reason, so the orchestration logic stays clean.
 */
@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    public void withdraw(UUID accountId, BigDecimal amount) {
        post("/accounts/" + accountId + "/withdraw", amount);
    }

    public void deposit(UUID accountId, BigDecimal amount) {
        post("/accounts/" + accountId + "/deposit", amount);
    }

    private void post(String path, BigDecimal amount) {
        restClient.post()
                .uri(path)
                .body(new AmountRequest(amount))
                .retrieve()
                .onStatus(status -> status.isError(), (request, response) -> {
                    throw new AccountClientException(reasonFor(HttpStatus.resolve(response.getStatusCode().value())));
                })
                .toBodilessEntity();
    }

    private String reasonFor(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "account not found";
        }
        if (status == HttpStatus.UNPROCESSABLE_ENTITY) {
            return "insufficient funds";
        }
        return "account-service error";
    }

    private record AmountRequest(BigDecimal amount) {
    }
}
