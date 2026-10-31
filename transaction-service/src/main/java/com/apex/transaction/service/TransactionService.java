package com.apex.transaction.service;

import com.apex.common.dto.TransactionResponse;
import com.apex.common.dto.TransferRequest;

import java.util.List;

public interface TransactionService {
    TransactionResponse transferFunds(TransferRequest request);
    TransactionResponse getTransactionById(String transactionId);
    List<TransactionResponse> getTransactionsByAccount(String accountNumber);
}
