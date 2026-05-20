package bg.tu_sofia.diploma.bank.web;

import bg.tu_sofia.diploma.bank.service.AccountService;
import bg.tu_sofia.diploma.bank.web.dto.AccountResponse;
import bg.tu_sofia.diploma.bank.web.dto.AmountRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public AccountResponse get(@AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(accountService.getOwnAccount(callerId(jwt)));
    }

    @PostMapping("/deposit")
    public AccountResponse deposit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.deposit(callerId(jwt), request.amount()));
    }

    @PostMapping("/withdraw")
    public AccountResponse withdraw(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.withdraw(callerId(jwt), request.amount()));
    }

    private static UUID callerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
