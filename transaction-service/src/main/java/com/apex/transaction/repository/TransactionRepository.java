package com.apex.transaction.repository;

import com.apex.transaction.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByTransactionId(String transactionId);
    Optional<TransactionRecord> findByIdempotencyKey(String idempotencyKey);
    List<TransactionRecord> findBySourceAccountNumberOrTargetAccountNumberOrderByCreatedAtDesc(
            String sourceAccountNumber, String targetAccountNumber);
}
