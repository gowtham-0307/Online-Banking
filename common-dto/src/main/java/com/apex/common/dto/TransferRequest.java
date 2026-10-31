package com.apex.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Target account number is required")
    private String targetAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String description;

    @NotBlank(message = "Idempotency key is required to prevent duplicate transfers")
    private String idempotencyKey;

    public TransferRequest() {
    }

    public TransferRequest(String sourceAccountNumber, String targetAccountNumber, BigDecimal amount,
                           String currency, String description, String idempotencyKey) {
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceAccountNumber;
        private String targetAccountNumber;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String idempotencyKey;

        public Builder sourceAccountNumber(String sourceAccountNumber) {
            this.sourceAccountNumber = sourceAccountNumber;
            return this;
        }

        public Builder targetAccountNumber(String targetAccountNumber) {
            this.targetAccountNumber = targetAccountNumber;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public TransferRequest build() {
            return new TransferRequest(sourceAccountNumber, targetAccountNumber, amount, currency, description, idempotencyKey);
        }
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
