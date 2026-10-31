package com.apex.transaction.kafka;

import com.apex.common.event.TransactionCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Component
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${apex.kafka.topics.transactions:bank.transactions}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishTransactionEvent(TransactionCreatedEvent event) {
        String corrId = MDC.get("correlationId");
        String key = event.getTransactionId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(topicName, key, event);
        if (corrId != null) {
            record.headers().add(new RecordHeader("X-Correlation-ID", corrId.getBytes(StandardCharsets.UTF_8)));
        }

        log.info("[KAFKA-PRODUCE-INIT] topic={} key={} transactionId={} amount={} corrId={}",
                topicName, key, event.getTransactionId(), event.getAmount(), corrId);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[KAFKA-PRODUCE-SUCCESS] topic={} partition={} offset={} transactionId={} corrId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getTransactionId(),
                        corrId);
            } else {
                log.error("[KAFKA-PRODUCE-ERROR] Failed to send transaction event to topic={} key={} corrId={}",
                        topicName, key, corrId, ex);
            }
        });
    }
}
