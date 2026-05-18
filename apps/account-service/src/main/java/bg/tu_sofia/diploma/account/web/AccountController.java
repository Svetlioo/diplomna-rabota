package bg.tu_sofia.diploma.account.web;

import bg.tu_sofia.diploma.account.service.AccountService;
import bg.tu_sofia.diploma.account.web.dto.AccountResponse;
import bg.tu_sofia.diploma.account.web.dto.AmountRequest;
import bg.tu_sofia.diploma.account.web.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(
                accountService.createAccount(request.ownerName(), request.initialBalance())
        );
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(accountService.getAccount(id));
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.deposit(id, request.amount()));
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.withdraw(id, request.amount()));
    }
}
