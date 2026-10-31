package com.apex.notification.service;

import com.apex.common.enums.NotificationChannel;
import com.apex.common.enums.NotificationStatus;
import com.apex.common.event.FraudAlertEvent;
import com.apex.common.event.TransactionCreatedEvent;
import com.apex.notification.entity.NotificationRecord;
import com.apex.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRepository repository;
    private final MeterRegistry meterRegistry;

    public NotificationDispatcher(NotificationRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public void dispatchTransactionNotification(TransactionCreatedEvent event) {
        String corrId = MDC.get("correlationId");
        if (corrId == null) {
            corrId = event.getCorrelationId();
        }

        log.info("[NOTIFICATION-DISPATCH-TX] txId={} from={} to={} amount={} corrId={}",
                event.getTransactionId(), event.getSourceAccountNumber(),
                event.getTargetAccountNumber(), event.getAmount(), corrId);

        String notificationId = "NOTIF-TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String subject = String.format("ApexBank Alert: Transfer of %s %s Processed", event.getAmount(), event.getCurrency());
        String body = String.format("Dear Customer, your transfer of %s %s from account %s to account %s was successfully executed. TxID: %s",
                event.getAmount(), event.getCurrency(), event.getSourceAccountNumber(), event.getTargetAccountNumber(), event.getTransactionId());

        NotificationRecord record = NotificationRecord.builder()
                .notificationId(notificationId)
                .recipientIdentifier(event.getSourceAccountNumber())
                .channel(NotificationChannel.EMAIL)
                .subject(subject)
                .messageBody(body)
                .status(NotificationStatus.SENT)
                .referenceId(event.getTransactionId())
                .correlationId(corrId)
                .sentAt(Instant.now())
                .build();

        repository.save(record);

        meterRegistry.counter("apex.banking.notifications.dispatched.total",
                "channel", "EMAIL", "type", "TRANSACTION").increment();

        log.info("[NOTIFICATION-SENT-SUCCESS] notifId={} recipient={} channel={} refId={} corrId={}",
                notificationId, event.getSourceAccountNumber(), NotificationChannel.EMAIL, event.getTransactionId(), corrId);
    }

    @Transactional
    public void dispatchFraudAlertNotification(FraudAlertEvent event) {
        String corrId = MDC.get("correlationId");
        if (corrId == null) {
            corrId = event.getCorrelationId();
        }

        log.warn("[NOTIFICATION-DISPATCH-FRAUD] alertId={} txId={} riskScore={} riskLevel={} corrId={}",
                event.getAlertId(), event.getTransactionId(), event.getRiskScore(), event.getRiskLevel(), corrId);

        String notificationId = "NOTIF-SEC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String subject = String.format("URGENT SECURITY ALERT: Suspicious Activity on Account %s", event.getSourceAccountNumber());
        String body = String.format("URGENT: A high-risk transaction (%s) with risk score %d/100 was flagged on account %s. Amount: %s. Rules: %s. Please review your account immediately.",
                event.getTransactionId(), event.getRiskScore(), event.getSourceAccountNumber(), event.getAmount(), event.getRuleTriggered());

        NotificationRecord record = NotificationRecord.builder()
                .notificationId(notificationId)
                .recipientIdentifier(event.getSourceAccountNumber())
                .channel(NotificationChannel.SMS)
                .subject(subject)
                .messageBody(body)
                .status(NotificationStatus.SENT)
                .referenceId(event.getAlertId())
                .correlationId(corrId)
                .sentAt(Instant.now())
                .build();

        repository.save(record);

        meterRegistry.counter("apex.banking.notifications.dispatched.total",
                "channel", "SMS", "type", "FRAUD_ALERT").increment();

        log.warn("[NOTIFICATION-SECURITY-SENT] notifId={} recipient={} channel=SMS alertId={} corrId={}",
                notificationId, event.getSourceAccountNumber(), event.getAlertId(), corrId);
    }
}
