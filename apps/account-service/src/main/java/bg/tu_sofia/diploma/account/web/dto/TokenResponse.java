package bg.tu_sofia.diploma.account.web.dto;

public record TokenResponse(String token, String tokenType) {

    public static TokenResponse bearer(String token) {
        return new TokenResponse(token, "Bearer");
    }
}
