package com.apex.gateway.config;

import com.apex.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/accounts")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> accountFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Account service is temporarily unavailable. Please retry shortly.", null)));
    }

    @GetMapping("/transactions")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> transactionFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Transaction service is temporarily unavailable. Please retry shortly.", null)));
    }
}
