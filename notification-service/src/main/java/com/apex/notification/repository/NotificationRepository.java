package com.apex.notification.repository;

import com.apex.notification.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {
    List<NotificationRecord> findByRecipientIdentifierOrderByCreatedAtDesc(String recipientIdentifier);
    List<NotificationRecord> findByReferenceId(String referenceId);
}
