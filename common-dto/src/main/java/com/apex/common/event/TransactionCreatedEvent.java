package com.apex.common.event;

import com.apex.common.enums.TransactionStatus;
import com.apex.common.enums.TransactionType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class TransactionCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String idempotencyKey;
    private String correlationId;
    private Instant createdAt;

    public TransactionCreatedEvent() {
    }

    public TransactionCreatedEvent(String transactionId, String sourceAccountNumber, String targetAccountNumber,
                                   BigDecimal amount, String currency, TransactionType type, TransactionStatus status,
                                   String description, String idempotencyKey, String correlationId, Instant createdAt) {
        this.transactionId = transactionId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.status = status;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionId;
        private String sourceAccountNumber;
        private String targetAccountNumber;
        private BigDecimal amount;
        private String currency;
        private TransactionType type;
        private TransactionStatus status;
        private String description;
        private String idempotencyKey;
        private String correlationId;
        private Instant createdAt;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

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

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
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

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TransactionCreatedEvent build() {
            return new TransactionCreatedEvent(transactionId, sourceAccountNumber, targetAccountNumber, amount,
                    currency, type, status, description, idempotencyKey, correlationId, createdAt);
        }
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
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

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
