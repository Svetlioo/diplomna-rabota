package bg.tu_sofia.diploma.bank.web;

import bg.tu_sofia.diploma.bank.service.AuthService;
import bg.tu_sofia.diploma.bank.web.dto.LoginRequest;
import bg.tu_sofia.diploma.bank.web.dto.RegisterRequest;
import bg.tu_sofia.diploma.bank.web.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return TokenResponse.bearer(authService.register(request.email(), request.password()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.bearer(authService.login(request.email(), request.password()));
    }
}
