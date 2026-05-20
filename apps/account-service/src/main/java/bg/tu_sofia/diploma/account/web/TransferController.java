package bg.tu_sofia.diploma.account.web;

import bg.tu_sofia.diploma.account.service.AccountService;
import bg.tu_sofia.diploma.account.web.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Money transfer from the caller's account to a target IBAN. The source is the
 * caller's own account, derived from the authenticated identity — never from the
 * request body — so a user can only ever move their own money. account-service
 * debits the source and credits the target inside a single, both-rows-locked
 * database transaction, so there is no intermediate state where money has left
 * one account but not yet reached the other.
 */
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest request) {
        accountService.transfer(UUID.fromString(jwt.getSubject()), request.toIban(), request.amount());
    }
}
