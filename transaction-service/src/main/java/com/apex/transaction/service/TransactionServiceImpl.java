package com.apex.transaction.service;

import com.apex.common.dto.AccountResponse;
import com.apex.common.dto.TransactionResponse;
import com.apex.common.dto.TransferRequest;
import com.apex.common.enums.AccountStatus;
import com.apex.common.enums.TransactionStatus;
import com.apex.common.enums.TransactionType;
import com.apex.common.event.TransactionCreatedEvent;
import com.apex.transaction.client.AccountServiceClient;
import com.apex.transaction.entity.TransactionRecord;
import com.apex.transaction.exception.DuplicateTransactionException;
import com.apex.transaction.exception.TransferExecutionException;
import com.apex.transaction.kafka.TransactionProducer;
import com.apex.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionProducer transactionProducer;
    private final Counter completedCounter;
    private final Counter failedCounter;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountServiceClient accountServiceClient,
                                  TransactionProducer transactionProducer,
                                  MeterRegistry meterRegistry) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.transactionProducer = transactionProducer;
        this.completedCounter = meterRegistry.counter("apex.banking.transactions.completed.total");
        this.failedCounter = meterRegistry.counter("apex.banking.transactions.failed.total");
    }

    @Override
    @Transactional
    public TransactionResponse transferFunds(TransferRequest request) {
        String corrId = MDC.get("correlationId");

        // 1. Idempotency Check: Return existing record if already processed
        Optional<TransactionRecord> existingRecord = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingRecord.isPresent()) {
            TransactionRecord tx = existingRecord.get();
            log.warn("[IDEMPOTENCY-HIT] Returning existing transactionId={} for key={} status={} corrId={}",
                    tx.getTransactionId(), request.getIdempotencyKey(), tx.getStatus(), corrId);
            return mapToResponse(tx);
        }

        // 2. Validate source != target
        if (request.getSourceAccountNumber().equalsIgnoreCase(request.getTargetAccountNumber())) {
            throw new TransferExecutionException("Source and target account cannot be identical.");
        }

        String txId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.info("[TRANSFER-START] txId={} from={} to={} amount={} curr={} key={} corrId={}",
                txId, request.getSourceAccountNumber(), request.getTargetAccountNumber(),
                request.getAmount(), request.getCurrency(), request.getIdempotencyKey(), corrId);

        // 3. Pre-flight Account Validation
        AccountResponse sourceAcc;
        AccountResponse targetAcc;
        try {
            sourceAcc = accountServiceClient.getAccount(request.getSourceAccountNumber());
            targetAcc = accountServiceClient.getAccount(request.getTargetAccountNumber());
        } catch (Exception ex) {
            log.error("[TRANSFER-PRECHECK-FAILED] Could not verify accounts - corrId={}", corrId, ex);
            throw new TransferExecutionException("Account validation failed: " + ex.getMessage(), ex);
        }

        if (sourceAcc == null || targetAcc == null) {
            throw new TransferExecutionException("One or both accounts do not exist.");
        }

        if (sourceAcc.getStatus() != AccountStatus.ACTIVE || targetAcc.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferExecutionException("Both accounts must be ACTIVE to transfer funds.");
        }

        // 4. Create initial PENDING Transaction Record
        TransactionRecord record = TransactionRecord.builder()
                .transactionId(txId)
                .idempotencyKey(request.getIdempotencyKey())
                .sourceAccountNumber(request.getSourceAccountNumber())
                .targetAccountNumber(request.getTargetAccountNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .type(TransactionType.INTERNAL_TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .correlationId(corrId)
                .build();
        record = transactionRepository.save(record);

        // 5. Execute Debit on Source Account
        try {
            accountServiceClient.debit(request.getSourceAccountNumber(), request.getAmount(),
                    txId, "Transfer to " + request.getTargetAccountNumber());
        } catch (Exception debitEx) {
            log.error("[TRANSFER-DEBIT-FAILED] txId={} sourceAccount={} corrId={}",
                    txId, request.getSourceAccountNumber(), corrId, debitEx);
            record.setStatus(TransactionStatus.FAILED);
            record.setFailureReason("Debit failed: " + debitEx.getMessage());
            transactionRepository.save(record);
            failedCounter.increment();
            throw new TransferExecutionException("Debit failed: " + debitEx.getMessage(), debitEx);
        }

        // 6. Execute Credit on Target Account (with automated compensation/rollback on failure)
        try {
            accountServiceClient.credit(request.getTargetAccountNumber(), request.getAmount(),
                    txId, "Transfer from " + request.getSourceAccountNumber());
        } catch (Exception creditEx) {
            log.error("[TRANSFER-CREDIT-FAILED] txId={} Initiating compensation rollback on source={} corrId={}",
                    txId, request.getSourceAccountNumber(), corrId, creditEx);
            try {
                // Compensating Transaction (Rollback debit)
                accountServiceClient.credit(request.getSourceAccountNumber(), request.getAmount(),
                        txId + "-COMPENSATION", "Rollback due to credit failure");
                log.info("[TRANSFER-COMPENSATION-SUCCESS] txId={} Rollback completed for source={}",
                        txId, request.getSourceAccountNumber());
            } catch (Exception compEx) {
                log.error("[TRANSFER-CRITICAL-COMPENSATION-FAILED] txId={} Manual intervention required!", txId, compEx);
            }

            record.setStatus(TransactionStatus.FAILED);
            record.setFailureReason("Credit to target account failed. Source refunded.");
            transactionRepository.save(record);
            failedCounter.increment();
            throw new TransferExecutionException("Credit failed. Source account has been refunded.", creditEx);
        }

        // 7. Mark COMPLETED
        record.setStatus(TransactionStatus.COMPLETED);
        record.setCompletedAt(Instant.now());
        TransactionRecord completed = transactionRepository.save(record);
        completedCounter.increment();

        log.info("[TRANSFER-SUCCESS] txId={} from={} to={} amount={} corrId={}",
                txId, request.getSourceAccountNumber(), request.getTargetAccountNumber(), request.getAmount(), corrId);

        // 8. Publish Kafka Event for asynchronous Fraud Evaluation and Notifications
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(completed.getTransactionId())
                .sourceAccountNumber(completed.getSourceAccountNumber())
                .targetAccountNumber(completed.getTargetAccountNumber())
                .amount(completed.getAmount())
                .currency(completed.getCurrency())
                .type(completed.getType())
                .status(completed.getStatus())
                .description(completed.getDescription())
                .idempotencyKey(completed.getIdempotencyKey())
                .correlationId(corrId)
                .createdAt(completed.getCreatedAt())
                .build();
        transactionProducer.publishTransactionEvent(event);

        return mapToResponse(completed);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String transactionId) {
        TransactionRecord record = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransferExecutionException("Transaction not found: " + transactionId));
        return mapToResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findBySourceAccountNumberOrTargetAccountNumberOrderByCreatedAtDesc(
                        accountNumber, accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(TransactionRecord r) {
        return TransactionResponse.builder()
                .transactionId(r.getTransactionId())
                .sourceAccountNumber(r.getSourceAccountNumber())
                .targetAccountNumber(r.getTargetAccountNumber())
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .type(r.getType())
                .status(r.getStatus())
                .description(r.getDescription())
                .idempotencyKey(r.getIdempotencyKey())
                .failureReason(r.getFailureReason())
                .createdAt(r.getCreatedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }
}
