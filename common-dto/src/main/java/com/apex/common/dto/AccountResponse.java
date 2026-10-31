package com.apex.common.dto;

import com.apex.common.enums.AccountStatus;
import com.apex.common.enums.AccountType;
import java.math.BigDecimal;
import java.time.Instant;

public class AccountResponse {
    private String accountNumber;
    private String customerId;
    private String customerName;
    private String email;
    private String phoneNumber;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private String currency;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public AccountResponse() {
    }

    public AccountResponse(String accountNumber, String customerId, String customerName, String email,
                           String phoneNumber, AccountType accountType, AccountStatus status, BigDecimal balance,
                           String currency, Long version, Instant createdAt, Instant updatedAt) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.accountType = accountType;
        this.status = status;
        this.balance = balance;
        this.currency = currency;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accountNumber;
        private String customerId;
        private String customerName;
        private String email;
        private String phoneNumber;
        private AccountType accountType;
        private AccountStatus status;
        private BigDecimal balance;
        private String currency;
        private Long version;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        public Builder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public Builder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AccountResponse build() {
            return new AccountResponse(accountNumber, customerId, customerName, email, phoneNumber,
                    accountType, status, balance, currency, version, createdAt, updatedAt);
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
