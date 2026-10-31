package com.apex.transaction.exception;

import com.apex.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateTransactionException ex) {
        String corrId = MDC.get("correlationId");
        log.warn("[DUPLICATE-IDEMPOTENCY-KEY] {} - corrId={}", ex.getMessage(), corrId);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), corrId));
    }

    @ExceptionHandler(TransferExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransferExecution(TransferExecutionException ex) {
        String corrId = MDC.get("correlationId");
        log.error("[TRANSFER-EXECUTION-FAILURE] {} - corrId={}", ex.getMessage(), corrId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), corrId));
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
                .body(ApiResponse.error("Internal processing error: " + ex.getMessage(), corrId));
    }
}
