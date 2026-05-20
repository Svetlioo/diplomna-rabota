package bg.tu_sofia.diploma.bank.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String email,

        @NotBlank
        String password
) {
}
