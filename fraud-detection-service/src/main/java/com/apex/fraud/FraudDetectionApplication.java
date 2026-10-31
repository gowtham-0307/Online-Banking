package com.apex.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
public class FraudDetectionApplication {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionApplication.class, args);
        log.info(">>> Apex Banking Fraud Detection Service initialized on port 8083 <<<");
    }
}
