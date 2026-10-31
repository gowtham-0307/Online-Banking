package com.apex.fraud.service;

import com.apex.common.enums.RiskLevel;
import com.apex.common.event.FraudAlertEvent;
import com.apex.common.event.TransactionCreatedEvent;
import com.apex.fraud.entity.FraudAssessmentRecord;
import com.apex.fraud.kafka.FraudAlertProducer;
import com.apex.fraud.repository.FraudAssessmentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class FraudRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudRuleEngine.class);

    private static final BigDecimal THRESHOLD_HIGH_VALUE = new BigDecimal("10000.00");
    private static final BigDecimal THRESHOLD_CRITICAL_VALUE = new BigDecimal("50000.00");

    private final FraudAssessmentRepository repository;
    private final FraudAlertProducer alertProducer;
    private final MeterRegistry meterRegistry;

    public FraudRuleEngine(FraudAssessmentRepository repository,
                           FraudAlertProducer alertProducer,
                           MeterRegistry meterRegistry) {
        this.repository = repository;
        this.alertProducer = alertProducer;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public FraudAssessmentRecord evaluate(TransactionCreatedEvent event) {
        String corrId = MDC.get("correlationId");
        if (corrId == null) {
            corrId = event.getCorrelationId();
            MDC.put("correlationId", corrId != null ? corrId : "kafka-fraud-eval");
        }

        log.info("[FRAUD-EVAL-START] txId={} from={} to={} amount={} corrId={}",
                event.getTransactionId(), event.getSourceAccountNumber(),
                event.getTargetAccountNumber(), event.getAmount(), corrId);

        int score = 0;
        StringBuilder rulesTriggered = new StringBuilder();

        // Rule 1: High Value Threshold
        if (event.getAmount().compareTo(THRESHOLD_CRITICAL_VALUE) >= 0) {
            score += 90;
            rulesTriggered.append("CRITICAL_AMOUNT_EXCEEDED_50K; ");
        } else if (event.getAmount().compareTo(THRESHOLD_HIGH_VALUE) >= 0) {
            score += 65;
            rulesTriggered.append("HIGH_VALUE_EXCEEDED_10K; ");
        }

        // Rule 2: Round number heuristic on high sums
        if (event.getAmount().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0 
                && event.getAmount().compareTo(new BigDecimal("5000")) >= 0) {
            score += 10;
            rulesTriggered.append("ROUND_HIGH_AMOUNT_ANOMALY; ");
        }

        RiskLevel riskLevel;
        if (score >= 85) {
            riskLevel = RiskLevel.CRITICAL;
        } else if (score >= 60) {
            riskLevel = RiskLevel.HIGH;
        } else if (score >= 30) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.LOW;
        }

        String alertId = "FRD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        FraudAssessmentRecord assessment = FraudAssessmentRecord.builder()
                .alertId(alertId)
                .transactionId(event.getTransactionId())
                .sourceAccountNumber(event.getSourceAccountNumber())
                .targetAccountNumber(event.getTargetAccountNumber())
                .amount(event.getAmount())
                .riskScore(score)
                .riskLevel(riskLevel)
                .ruleTriggered(rulesTriggered.length() > 0 ? rulesTriggered.toString() : "NONE_PASS")
                .notes(String.format("Evaluated with score %d. Status: %s", score, riskLevel))
                .correlationId(corrId)
                .build();

        FraudAssessmentRecord saved = repository.save(assessment);

        // Record metrics
        meterRegistry.counter("apex.banking.fraud.evaluations.total", "risk_level", riskLevel.name()).increment();

        if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL) {
            log.warn("[FRAUD-SUSPICIOUS-DETECTED] alertId={} txId={} score={} riskLevel={} rules={} corrId={}",
                    alertId, event.getTransactionId(), score, riskLevel, rulesTriggered, corrId);

            FraudAlertEvent alertEvent = FraudAlertEvent.builder()
                    .alertId(alertId)
                    .transactionId(event.getTransactionId())
                    .sourceAccountNumber(event.getSourceAccountNumber())
                    .targetAccountNumber(event.getTargetAccountNumber())
                    .amount(event.getAmount())
                    .riskScore(score)
                    .riskLevel(riskLevel)
                    .ruleTriggered(rulesTriggered.toString())
                    .notes(saved.getNotes())
                    .correlationId(corrId)
                    .flaggedAt(Instant.now())
                    .build();

            alertProducer.publishAlert(alertEvent);
        } else {
            log.info("[FRAUD-EVAL-CLEAN] txId={} score={} riskLevel={} corrId={}",
                    event.getTransactionId(), score, riskLevel, corrId);
        }

        return saved;
    }
}
