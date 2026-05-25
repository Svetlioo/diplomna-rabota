package bg.tu_sofia.diploma.bank.web;

import bg.tu_sofia.diploma.bank.service.AuthService;
import bg.tu_sofia.diploma.bank.web.dto.LoginRequest;
import bg.tu_sofia.diploma.bank.web.dto.RegisterRequest;
import bg.tu_sofia.diploma.bank.web.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_NAME = "token";

    private final AuthService authService;

    @Value("${security.jwt.ttl:PT12H}")
    private Duration tokenTtl;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return tokenResponse(authService.register(request.email(), request.password()), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(authService.login(request.email(), request.password()), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, tokenCookie("", Duration.ZERO).toString())
                .build();
    }

    // Returns the token in the body (for API clients) and as an httpOnly cookie
    // (for the browser, which then never touches the JWT in JS).
    private ResponseEntity<TokenResponse> tokenResponse(String token, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, tokenCookie(token, tokenTtl).toString())
                .body(TokenResponse.bearer(token));
    }

    private ResponseCookie tokenCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
