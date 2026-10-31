package com.apex.transaction.client;

import com.apex.common.dto.AccountResponse;
import com.apex.common.dto.ApiResponse;
import com.apex.common.dto.BalanceUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);

    private final RestClient restClient;

    public AccountServiceClient(@Value("${apex.services.account-url:http://localhost:8081}") String accountServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }

    public AccountResponse getAccount(String accountNumber) {
        String corrId = MDC.get("correlationId");
        log.debug("[CLIENT-CALL-ACCOUNT-GET] calling account-service for account={} corrId={}", accountNumber, corrId);

        ApiResponse<AccountResponse> response = restClient.get()
                .uri("/api/v1/accounts/{accountNumber}", accountNumber)
                .header("X-Correlation-ID", corrId != null ? corrId : "")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {});

        return response != null ? response.getData() : null;
    }

    public AccountResponse debit(String accountNumber, BigDecimal amount, String refId, String description) {
        String corrId = MDC.get("correlationId");
        log.info("[CLIENT-CALL-ACCOUNT-DEBIT] initiating debit for account={} amount={} ref={} corrId={}",
                accountNumber, amount, refId, corrId);

        BalanceUpdateRequest req = BalanceUpdateRequest.builder()
                .accountNumber(accountNumber)
                .amount(amount)
                .referenceId(refId)
                .description(description)
                .build();

        ApiResponse<AccountResponse> response = restClient.post()
                .uri("/api/v1/accounts/debit")
                .header("X-Correlation-ID", corrId != null ? corrId : "")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {});

        return response != null ? response.getData() : null;
    }

    public AccountResponse credit(String accountNumber, BigDecimal amount, String refId, String description) {
        String corrId = MDC.get("correlationId");
        log.info("[CLIENT-CALL-ACCOUNT-CREDIT] initiating credit for account={} amount={} ref={} corrId={}",
                accountNumber, amount, refId, corrId);

        BalanceUpdateRequest req = BalanceUpdateRequest.builder()
                .accountNumber(accountNumber)
                .amount(amount)
                .referenceId(refId)
                .description(description)
                .build();

        ApiResponse<AccountResponse> response = restClient.post()
                .uri("/api/v1/accounts/credit")
                .header("X-Correlation-ID", corrId != null ? corrId : "")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {});

        return response != null ? response.getData() : null;
    }
}
