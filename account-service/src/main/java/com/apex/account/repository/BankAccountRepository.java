package com.apex.account.repository;

import com.apex.account.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findByCustomerId(String customerId);
    boolean existsByAccountNumber(String accountNumber);
}
