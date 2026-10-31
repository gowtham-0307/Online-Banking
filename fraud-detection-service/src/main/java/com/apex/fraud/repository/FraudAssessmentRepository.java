package com.apex.fraud.repository;

import com.apex.common.enums.RiskLevel;
import com.apex.fraud.entity.FraudAssessmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAssessmentRepository extends JpaRepository<FraudAssessmentRecord, Long> {
    List<FraudAssessmentRecord> findByTransactionId(String transactionId);
    List<FraudAssessmentRecord> findByRiskLevelInOrderByEvaluatedAtDesc(List<RiskLevel> riskLevels);
    List<FraudAssessmentRecord> findBySourceAccountNumberOrderByEvaluatedAtDesc(String sourceAccountNumber);
}
