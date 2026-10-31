package com.apex.fraud.controller;

import com.apex.common.dto.ApiResponse;
import com.apex.common.enums.RiskLevel;
import com.apex.fraud.entity.FraudAssessmentRecord;
import com.apex.fraud.repository.FraudAssessmentRepository;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudController {

    private final FraudAssessmentRepository repository;

    public FraudController(FraudAssessmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<FraudAssessmentRecord>>> getAllAlerts() {
        String corrId = MDC.get("correlationId");
        List<FraudAssessmentRecord> list = repository.findAll();
        return ResponseEntity.ok(ApiResponse.ok("Fraud assessments retrieved", list, corrId));
    }

    @GetMapping("/alerts/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<List<FraudAssessmentRecord>>> getByTransaction(@PathVariable String transactionId) {
        String corrId = MDC.get("correlationId");
        List<FraudAssessmentRecord> list = repository.findByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction fraud records retrieved", list, corrId));
    }

    @GetMapping("/alerts/high-risk")
    public ResponseEntity<ApiResponse<List<FraudAssessmentRecord>>> getHighRiskAlerts() {
        String corrId = MDC.get("correlationId");
        List<FraudAssessmentRecord> list = repository.findByRiskLevelInOrderByEvaluatedAtDesc(
                Arrays.asList(RiskLevel.HIGH, RiskLevel.CRITICAL));
        return ResponseEntity.ok(ApiResponse.ok("High risk fraud alerts retrieved", list, corrId));
    }
}
