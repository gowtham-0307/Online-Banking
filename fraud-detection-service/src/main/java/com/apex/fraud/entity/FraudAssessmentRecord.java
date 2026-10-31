package com.apex.fraud.entity;

import com.apex.common.enums.RiskLevel;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fraud_assessments", indexes = {
        @Index(name = "idx_fraud_tx_id", columnList = "transaction_id"),
        @Index(name = "idx_fraud_risk_level", columnList = "risk_level"),
        @Index(name = "idx_fraud_source_acc", columnList = "source_account_number")
})
@EntityListeners(AuditingEntityListener.class)
public class FraudAssessmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true, length = 64)
    private String alertId;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "source_account_number", nullable = false, length = 32)
    private String sourceAccountNumber;

    @Column(name = "target_account_number", nullable = false, length = 32)
    private String targetAccountNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(name = "rule_triggered", length = 255)
    private String ruleTriggered;

    @Column(name = "notes", length = 1024)
    private String notes;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @CreatedDate
    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;

    public FraudAssessmentRecord() {
    }

    public FraudAssessmentRecord(Long id, String alertId, String transactionId, String sourceAccountNumber,
                                 String targetAccountNumber, BigDecimal amount, Integer riskScore,
                                 RiskLevel riskLevel, String ruleTriggered, String notes,
                                 String correlationId, Instant evaluatedAt) {
        this.id = id;
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.ruleTriggered = ruleTriggered;
        this.notes = notes;
        this.correlationId = correlationId;
        this.evaluatedAt = evaluatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String alertId;
        private String transactionId;
        private String sourceAccountNumber;
        private String targetAccountNumber;
        private BigDecimal amount;
        private Integer riskScore;
        private RiskLevel riskLevel;
        private String ruleTriggered;
        private String notes;
        private String correlationId;
        private Instant evaluatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder alertId(String alertId) {
            this.alertId = alertId;
            return this;
        }

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

        public Builder riskScore(Integer riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder ruleTriggered(String ruleTriggered) {
            this.ruleTriggered = ruleTriggered;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder evaluatedAt(Instant evaluatedAt) {
            this.evaluatedAt = evaluatedAt;
            return this;
        }

        public FraudAssessmentRecord build() {
            return new FraudAssessmentRecord(id, alertId, transactionId, sourceAccountNumber, targetAccountNumber,
                    amount, riskScore, riskLevel, ruleTriggered, notes, correlationId, evaluatedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
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

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }

    public void setRuleTriggered(String ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
