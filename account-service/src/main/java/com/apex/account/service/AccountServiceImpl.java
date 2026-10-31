package com.apex.account.service;

import com.apex.account.entity.AccountAuditLog;
import com.apex.account.entity.BankAccount;
import com.apex.account.exception.AccountNotFoundException;
import com.apex.account.exception.AccountSuspendedException;
import com.apex.account.exception.InsufficientFundsException;
import com.apex.account.repository.AccountAuditLogRepository;
import com.apex.account.repository.BankAccountRepository;
import com.apex.common.dto.AccountResponse;
import com.apex.common.dto.BalanceUpdateRequest;
import com.apex.common.dto.CreateAccountRequest;
import com.apex.common.enums.AccountStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final BankAccountRepository accountRepository;
    private final AccountAuditLogRepository auditLogRepository;
    private final Counter accountCreatedCounter;
    private final Counter accountDebitCounter;
    private final Counter accountCreditCounter;
    private final SecureRandom random = new SecureRandom();

    public AccountServiceImpl(BankAccountRepository accountRepository,
                              AccountAuditLogRepository auditLogRepository,
                              MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
        this.accountCreatedCounter = meterRegistry.counter("apex.banking.accounts.created.total");
        this.accountDebitCounter = meterRegistry.counter("apex.banking.accounts.debit.total");
        this.accountCreditCounter = meterRegistry.counter("apex.banking.accounts.credit.total");
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest req) {
        String corrId = MDC.get("correlationId");
        log.info("[ACCOUNT-CREATION-INIT] customerId={} name={} type={} initialDeposit={} curr={} corrId={}",
                req.getCustomerId(), req.getCustomerName(), req.getAccountType(), req.getInitialDeposit(), req.getCurrency(), corrId);

        String generatedAccountNumber = generateUniqueAccountNumber();

        BankAccount account = BankAccount.builder()
                .accountNumber(generatedAccountNumber)
                .customerId(req.getCustomerId())
                .customerName(req.getCustomerName())
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .accountType(req.getAccountType())
                .status(AccountStatus.ACTIVE)
                .balance(req.getInitialDeposit() != null ? req.getInitialDeposit() : BigDecimal.ZERO)
                .currency(req.getCurrency().toUpperCase())
                .build();

        BankAccount saved = accountRepository.save(account);

        // Audit Log Entry
        AccountAuditLog audit = AccountAuditLog.builder()
                .accountNumber(saved.getAccountNumber())
                .actionType("ACCOUNT_CREATED")
                .amount(saved.getBalance())
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(saved.getBalance())
                .referenceId("INIT-" + saved.getAccountNumber())
                .description("Initial account creation with deposit")
                .build();
        auditLogRepository.save(audit);

        accountCreatedCounter.increment();
        log.info("[ACCOUNT-CREATION-SUCCESS] accountNumber={} customerId={} balance={} corrId={}",
                saved.getAccountNumber(), saved.getCustomerId(), saved.getBalance(), corrId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        String corrId = MDC.get("correlationId");
        log.debug("[ACCOUNT-FETCH] accountNumber={} corrId={}", accountNumber, corrId);

        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(String customerId) {
        String corrId = MDC.get("correlationId");
        log.debug("[ACCOUNT-FETCH-BY-CUSTOMER] customerId={} corrId={}", customerId, corrId);

        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse debitAccount(BalanceUpdateRequest req) {
        String corrId = MDC.get("correlationId");
        log.info("[ACCOUNT-DEBIT-INIT] accountNumber={} amount={} ref={} corrId={}",
                req.getAccountNumber(), req.getAmount(), req.getReferenceId(), corrId);

        BankAccount account = accountRepository.findByAccountNumber(req.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Debit failed: Account not found " + req.getAccountNumber()));

        validateAccountActive(account);

        if (account.getBalance().compareTo(req.getAmount()) < 0) {
            log.warn("[ACCOUNT-DEBIT-FAILED-INSUFFICIENT-FUNDS] accountNumber={} currentBalance={} requestedDebit={} corrId={}",
                    account.getAccountNumber(), account.getBalance(), req.getAmount(), corrId);
            throw new InsufficientFundsException(String.format(
                    "Insufficient balance in account %s. Current: %s, Requested: %s",
                    account.getAccountNumber(), account.getBalance(), req.getAmount()));
        }

        BigDecimal beforeBalance = account.getBalance();
        BigDecimal afterBalance = beforeBalance.subtract(req.getAmount());
        account.setBalance(afterBalance);

        BankAccount updated = accountRepository.save(account);

        AccountAuditLog audit = AccountAuditLog.builder()
                .accountNumber(updated.getAccountNumber())
                .actionType("DEBIT")
                .amount(req.getAmount())
                .balanceBefore(beforeBalance)
                .balanceAfter(afterBalance)
                .referenceId(req.getReferenceId())
                .description(req.getDescription() != null ? req.getDescription() : "Account debit")
                .build();
        auditLogRepository.save(audit);

        accountDebitCounter.increment();
        log.info("[ACCOUNT-DEBIT-SUCCESS] accountNumber={} debitAmount={} newBalance={} ref={} version={} corrId={}",
                updated.getAccountNumber(), req.getAmount(), updated.getBalance(), req.getReferenceId(), updated.getVersion(), corrId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public AccountResponse creditAccount(BalanceUpdateRequest req) {
        String corrId = MDC.get("correlationId");
        log.info("[ACCOUNT-CREDIT-INIT] accountNumber={} amount={} ref={} corrId={}",
                req.getAccountNumber(), req.getAmount(), req.getReferenceId(), corrId);

        BankAccount account = accountRepository.findByAccountNumber(req.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Credit failed: Account not found " + req.getAccountNumber()));

        validateAccountActive(account);

        BigDecimal beforeBalance = account.getBalance();
        BigDecimal afterBalance = beforeBalance.add(req.getAmount());
        account.setBalance(afterBalance);

        BankAccount updated = accountRepository.save(account);

        AccountAuditLog audit = AccountAuditLog.builder()
                .accountNumber(updated.getAccountNumber())
                .actionType("CREDIT")
                .amount(req.getAmount())
                .balanceBefore(beforeBalance)
                .balanceAfter(afterBalance)
                .referenceId(req.getReferenceId())
                .description(req.getDescription() != null ? req.getDescription() : "Account credit")
                .build();
        auditLogRepository.save(audit);

        accountCreditCounter.increment();
        log.info("[ACCOUNT-CREDIT-SUCCESS] accountNumber={} creditAmount={} newBalance={} ref={} version={} corrId={}",
                updated.getAccountNumber(), req.getAmount(), updated.getBalance(), req.getReferenceId(), updated.getVersion(), corrId);

        return mapToResponse(updated);
    }

    private void validateAccountActive(BankAccount account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountSuspendedException(String.format(
                    "Account %s is not active. Current status: %s",
                    account.getAccountNumber(), account.getStatus()));
        }
    }

    private String generateUniqueAccountNumber() {
        String acc;
        do {
            long number = 1000000000L + (long)(random.nextDouble() * 8999999999L);
            acc = "APX" + number;
        } while (accountRepository.existsByAccountNumber(acc));
        return acc;
    }

    private AccountResponse mapToResponse(BankAccount a) {
        return AccountResponse.builder()
                .accountNumber(a.getAccountNumber())
                .customerId(a.getCustomerId())
                .customerName(a.getCustomerName())
                .email(a.getEmail())
                .phoneNumber(a.getPhoneNumber())
                .accountType(a.getAccountType())
                .status(a.getStatus())
                .balance(a.getBalance())
                .currency(a.getCurrency())
                .version(a.getVersion())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
