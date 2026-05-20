package bg.tu_sofia.diploma.transaction.web;

import bg.tu_sofia.diploma.transaction.service.TransactionService;
import bg.tu_sofia.diploma.transaction.web.dto.CreateTransferRequest;
import bg.tu_sofia.diploma.transaction.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransferRequest request) {
        return TransactionResponse.from(
                transactionService.transfer(
                        request.fromAccountId(), request.toAccountId(), request.amount(), request.currency())
        );
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) {
        return TransactionResponse.from(transactionService.getTransaction(id));
    }

    @GetMapping
    public List<TransactionResponse> listByAccount(@RequestParam UUID accountId) {
        return transactionService.getByAccount(accountId).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
