package com.apex.transaction.entity;

import com.apex.common.enums.TransactionStatus;
import com.apex.common.enums.TransactionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true),
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_source_account", columnList = "source_account_number"),
        @Index(name = "idx_target_account", columnList = "target_account_number"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "source_account_number", nullable = false, length = 32)
    private String sourceAccountNumber;

    @Column(name = "target_account_number", nullable = false, length = 32)
    private String targetAccountNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransactionStatus status;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public TransactionRecord() {
    }

    public TransactionRecord(Long id, String transactionId, String idempotencyKey, String sourceAccountNumber,
                             String targetAccountNumber, BigDecimal amount, String currency, TransactionType type,
                             TransactionStatus status, String description, String failureReason,
                             String correlationId, Instant createdAt, Instant completedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.status = status;
        this.description = description;
        this.failureReason = failureReason;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String transactionId;
        private String idempotencyKey;
        private String sourceAccountNumber;
        private String targetAccountNumber;
        private BigDecimal amount;
        private String currency;
        private TransactionType type;
        private TransactionStatus status;
        private String description;
        private String failureReason;
        private String correlationId;
        private Instant createdAt;
        private Instant completedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
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

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
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

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public TransactionRecord build() {
            return new TransactionRecord(id, transactionId, idempotencyKey, sourceAccountNumber, targetAccountNumber,
                    amount, currency, type, status, description, failureReason, correlationId, createdAt, completedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
