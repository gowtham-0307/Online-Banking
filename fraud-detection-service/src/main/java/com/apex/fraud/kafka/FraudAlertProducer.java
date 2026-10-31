package com.apex.fraud.kafka;

import com.apex.common.event.FraudAlertEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class FraudAlertProducer {

    private static final Logger log = LoggerFactory.getLogger(FraudAlertProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String alertTopic;

    public FraudAlertProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              @Value("${apex.kafka.topics.fraud-alerts:bank.fraud-alerts}") String alertTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.alertTopic = alertTopic;
    }

    public void publishAlert(FraudAlertEvent alert) {
        String corrId = MDC.get("correlationId");
        String key = alert.getAlertId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(alertTopic, key, alert);
        if (corrId != null) {
            record.headers().add(new RecordHeader("X-Correlation-ID", corrId.getBytes(StandardCharsets.UTF_8)));
        }

        log.warn("[FRAUD-ALERT-PUBLISH] topic={} alertId={} txId={} riskScore={} riskLevel={} corrId={}",
                alertTopic, alert.getAlertId(), alert.getTransactionId(), alert.getRiskScore(), alert.getRiskLevel(), corrId);

        kafkaTemplate.send(record).whenComplete((res, ex) -> {
            if (ex == null) {
                log.info("[FRAUD-ALERT-DISPATCHED] alertId={} offset={} corrId={}",
                        alert.getAlertId(), res.getRecordMetadata().offset(), corrId);
            } else {
                log.error("[FRAUD-ALERT-DISPATCH-ERROR] Failed to send alertId={} corrId={}",
                        alert.getAlertId(), corrId, ex);
            }
        });
    }
}
