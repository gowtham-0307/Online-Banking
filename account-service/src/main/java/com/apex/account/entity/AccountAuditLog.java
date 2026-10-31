package com.apex.account.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_audit_logs", indexes = {
        @Index(name = "idx_audit_account_number", columnList = "account_number"),
        @Index(name = "idx_audit_reference_id", columnList = "reference_id")
})
@EntityListeners(AuditingEntityListener.class)
public class AccountAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 32)
    private String accountNumber;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "description", length = 255)
    private String description;

    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    public AccountAuditLog() {
    }

    public AccountAuditLog(Long id, String accountNumber, String actionType, BigDecimal amount,
                           BigDecimal balanceBefore, BigDecimal balanceAfter, String referenceId,
                           String description, Instant timestamp) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.actionType = actionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.description = description;
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String accountNumber;
        private String actionType;
        private BigDecimal amount;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private String referenceId;
        private String description;
        private Instant timestamp;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder balanceBefore(BigDecimal balanceBefore) {
            this.balanceBefore = balanceBefore;
            return this;
        }

        public Builder balanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AccountAuditLog build() {
            return new AccountAuditLog(id, accountNumber, actionType, amount, balanceBefore, balanceAfter,
                    referenceId, description, timestamp);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
