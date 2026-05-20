package bg.tu_sofia.diploma.bank.web;

import bg.tu_sofia.diploma.bank.domain.Transaction;
import bg.tu_sofia.diploma.bank.service.TransactionService;
import bg.tu_sofia.diploma.bank.web.dto.CreateTransferRequest;
import bg.tu_sofia.diploma.bank.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTransferRequest request) {
        return TransactionResponse.from(
                transactionService.transfer(callerId(jwt), request.toIban(), request.amount()));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return TransactionResponse.from(transactionService.getOwnTransaction(id, callerId(jwt)));
    }

    @GetMapping
    public PagedModel<TransactionResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Transaction> page = transactionService.getOwnTransactions(callerId(jwt), pageable);
        return new PagedModel<>(page.map(TransactionResponse::from));
    }

    private static UUID callerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
