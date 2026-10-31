package com.apex.account.controller;

import com.apex.account.service.AccountService;
import com.apex.common.dto.AccountResponse;
import com.apex.common.dto.ApiResponse;
import com.apex.common.dto.BalanceUpdateRequest;
import com.apex.common.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        String corrId = MDC.get("correlationId");
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account successfully created", response, corrId));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNumber) {
        String corrId = MDC.get("correlationId");
        AccountResponse response = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok("Account retrieved successfully", response, corrId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByCustomer(@PathVariable String customerId) {
        String corrId = MDC.get("correlationId");
        List<AccountResponse> response = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.ok("Customer accounts retrieved successfully", response, corrId));
    }

    @PostMapping("/debit")
    public ResponseEntity<ApiResponse<AccountResponse>> debitAccount(@Valid @RequestBody BalanceUpdateRequest request) {
        String corrId = MDC.get("correlationId");
        AccountResponse response = accountService.debitAccount(request);
        return ResponseEntity.ok(ApiResponse.ok("Account successfully debited", response, corrId));
    }

    @PostMapping("/credit")
    public ResponseEntity<ApiResponse<AccountResponse>> creditAccount(@Valid @RequestBody BalanceUpdateRequest request) {
        String corrId = MDC.get("correlationId");
        AccountResponse response = accountService.creditAccount(request);
        return ResponseEntity.ok(ApiResponse.ok("Account successfully credited", response, corrId));
    }
}
