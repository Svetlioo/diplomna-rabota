package bg.tu_sofia.diploma.transaction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient accountRestClient(@Value("${account-service.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                // Relay the caller's Bearer token to account-service, whose
                // /transfers endpoint is itself secured and enforces ownership.
                .requestInterceptor((request, body, execution) -> {
                    String authorization = currentAuthorizationHeader();
                    if (authorization != null) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    private static String currentAuthorizationHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
