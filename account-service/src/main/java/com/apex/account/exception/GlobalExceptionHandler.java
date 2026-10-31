package com.apex.account.exception;

import com.apex.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(AccountNotFoundException ex) {
        String corrId = MDC.get("correlationId");
        log.warn("[ACCOUNT-NOT-FOUND] {} - corrId={}", ex.getMessage(), corrId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), corrId));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientFunds(InsufficientFundsException ex) {
        String corrId = MDC.get("correlationId");
        log.warn("[INSUFFICIENT-FUNDS] {} - corrId={}", ex.getMessage(), corrId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), corrId));
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSuspended(AccountSuspendedException ex) {
        String corrId = MDC.get("correlationId");
        log.warn("[ACCOUNT-SUSPENDED] {} - corrId={}", ex.getMessage(), corrId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), corrId));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        String corrId = MDC.get("correlationId");
        log.error("[OPTIMISTIC-LOCK-CONFLICT] Concurrent update conflict detected - corrId={}", corrId);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Concurrent update conflict. Please retry the transaction.", corrId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        String corrId = MDC.get("correlationId");
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("[VALIDATION-ERROR] {} fields invalid - corrId={}", errors.size(), corrId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .correlationId(corrId)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        String corrId = MDC.get("correlationId");
        log.error("[INTERNAL-ERROR] Unhandled exception occurred - corrId={}", corrId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An internal error occurred: " + ex.getMessage(), corrId));
    }
}
