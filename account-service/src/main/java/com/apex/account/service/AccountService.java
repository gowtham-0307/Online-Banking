package com.apex.account.service;

import com.apex.common.dto.AccountResponse;
import com.apex.common.dto.BalanceUpdateRequest;
import com.apex.common.dto.CreateAccountRequest;

import java.util.List;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);
    AccountResponse getAccountByNumber(String accountNumber);
    List<AccountResponse> getAccountsByCustomerId(String customerId);
    AccountResponse debitAccount(BalanceUpdateRequest request);
    AccountResponse creditAccount(BalanceUpdateRequest request);
}
