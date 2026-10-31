package com.apex.common.dto;

import com.apex.common.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Customer full name is required")
    private String customerName;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    @NotBlank(message = "Currency is required (e.g. USD, EUR)")
    private String currency;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String customerId, String customerName, String email, String phoneNumber,
                                AccountType accountType, BigDecimal initialDeposit, String currency) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.accountType = accountType;
        this.initialDeposit = initialDeposit;
        this.currency = currency;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String customerId;
        private String customerName;
        private String email;
        private String phoneNumber;
        private AccountType accountType;
        private BigDecimal initialDeposit;
        private String currency;

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

        public Builder initialDeposit(BigDecimal initialDeposit) {
            this.initialDeposit = initialDeposit;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public CreateAccountRequest build() {
            return new CreateAccountRequest(customerId, customerName, email, phoneNumber, accountType, initialDeposit, currency);
        }
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

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
