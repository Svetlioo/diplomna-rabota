package bg.tu_sofia.diploma.transaction.web;

import bg.tu_sofia.diploma.transaction.domain.Transaction;
import bg.tu_sofia.diploma.transaction.service.TransactionService;
import bg.tu_sofia.diploma.transaction.web.dto.CreateTransferRequest;
import bg.tu_sofia.diploma.transaction.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public PagedModel<TransactionResponse> list(
            @RequestParam(required = false) UUID accountId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Transaction> page = accountId == null
                ? transactionService.getAll(pageable)
                : transactionService.getByAccount(accountId, pageable);
        return new PagedModel<>(page.map(TransactionResponse::from));
    }
}
