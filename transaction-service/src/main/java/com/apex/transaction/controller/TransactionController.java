package com.apex.transaction.controller;

import com.apex.common.dto.ApiResponse;
import com.apex.common.dto.TransactionResponse;
import com.apex.common.dto.TransferRequest;
import com.apex.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transferFunds(@Valid @RequestBody TransferRequest request) {
        String corrId = MDC.get("correlationId");
        TransactionResponse response = transactionService.transferFunds(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transfer successfully executed", response, corrId));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable String transactionId) {
        String corrId = MDC.get("correlationId");
        TransactionResponse response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction retrieved successfully", response, corrId));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountTransactions(@PathVariable String accountNumber) {
        String corrId = MDC.get("correlationId");
        List<TransactionResponse> response = transactionService.getTransactionsByAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok("Account transactions retrieved successfully", response, corrId));
    }
}
