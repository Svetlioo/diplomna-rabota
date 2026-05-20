package bg.tu_sofia.diploma.account.web;

import bg.tu_sofia.diploma.account.service.AccountService;
import bg.tu_sofia.diploma.account.web.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Money transfer between two accounts. Unlike the per-account deposit/withdraw
 * endpoints, a transfer is one atomic operation: account-service debits the
 * source and credits the target inside a single database transaction, locking
 * both rows. The orchestration lives here — not in a caller stitching two calls
 * together — precisely so there is no intermediate state where money has left
 * one account but not yet reached the other.
 */
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transfer(
                request.fromAccountId(), request.toAccountId(), request.amount(), request.currency());
    }
}
