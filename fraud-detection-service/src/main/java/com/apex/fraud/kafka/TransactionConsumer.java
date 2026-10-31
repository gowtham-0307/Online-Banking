package com.apex.fraud.kafka;

import com.apex.common.event.TransactionCreatedEvent;
import com.apex.fraud.service.FraudRuleEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

    private final FraudRuleEngine fraudRuleEngine;

    public TransactionConsumer(FraudRuleEngine fraudRuleEngine) {
        this.fraudRuleEngine = fraudRuleEngine;
    }

    @KafkaListener(topics = "${apex.kafka.topics.transactions:bank.transactions}", groupId = "fraud-detection-group")
    public void consumeTransactionEvent(ConsumerRecord<String, TransactionCreatedEvent> record) {
        String corrId = null;
        if (record.headers().lastHeader("X-Correlation-ID") != null) {
            corrId = new String(record.headers().lastHeader("X-Correlation-ID").value(), StandardCharsets.UTF_8);
        }
        if (corrId == null && record.value() != null) {
            corrId = record.value().getCorrelationId();
        }
        if (corrId == null) {
            corrId = "kafka-msg-" + record.offset();
        }

        MDC.put("correlationId", corrId);

        try {
            TransactionCreatedEvent event = record.value();
            log.info("[KAFKA-CONSUME-TRANSACTION] partition={} offset={} key={} txId={} amount={} corrId={}",
                    record.partition(), record.offset(), record.key(),
                    event.getTransactionId(), event.getAmount(), corrId);

            fraudRuleEngine.evaluate(event);
        } catch (Exception ex) {
            log.error("[KAFKA-CONSUME-ERROR] Failed to process transaction message at offset={} corrId={}",
                    record.offset(), corrId, ex);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
