package com.apex.account.repository;

import com.apex.account.entity.AccountAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountAuditLogRepository extends JpaRepository<AccountAuditLog, Long> {
    List<AccountAuditLog> findByAccountNumberOrderByTimestampDesc(String accountNumber);
}
