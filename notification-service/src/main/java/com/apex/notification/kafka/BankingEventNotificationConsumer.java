package com.apex.notification.kafka;

import com.apex.common.event.FraudAlertEvent;
import com.apex.common.event.TransactionCreatedEvent;
import com.apex.notification.service.NotificationDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class BankingEventNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(BankingEventNotificationConsumer.class);

    private final NotificationDispatcher notificationDispatcher;

    public BankingEventNotificationConsumer(NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;
    }

    @KafkaListener(topics = "${apex.kafka.topics.transactions:bank.transactions}",
                   groupId = "notification-transaction-group",
                   containerFactory = "transactionKafkaListenerContainerFactory")
    public void handleTransactionEvent(ConsumerRecord<String, TransactionCreatedEvent> record) {
        extractCorrelationId(record);
        try {
            TransactionCreatedEvent event = record.value();
            log.info("[NOTIFICATION-CONSUMER-TX] received transactionId={} corrId={}",
                    event.getTransactionId(), MDC.get("correlationId"));
            notificationDispatcher.dispatchTransactionNotification(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @KafkaListener(topics = "${apex.kafka.topics.fraud-alerts:bank.fraud-alerts}",
                   groupId = "notification-fraud-group",
                   containerFactory = "fraudKafkaListenerContainerFactory")
    public void handleFraudAlertEvent(ConsumerRecord<String, FraudAlertEvent> record) {
        extractCorrelationId(record);
        try {
            FraudAlertEvent alert = record.value();
            log.warn("[NOTIFICATION-CONSUMER-FRAUD] received alertId={} txId={} corrId={}",
                    alert.getAlertId(), alert.getTransactionId(), MDC.get("correlationId"));
            notificationDispatcher.dispatchFraudAlertNotification(alert);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void extractCorrelationId(ConsumerRecord<String, ?> record) {
        String corrId = null;
        if (record.headers().lastHeader("X-Correlation-ID") != null) {
            corrId = new String(record.headers().lastHeader("X-Correlation-ID").value(), StandardCharsets.UTF_8);
        }
        if (corrId == null) {
            corrId = "notif-rx-" + record.offset();
        }
        MDC.put("correlationId", corrId);
    }
}
