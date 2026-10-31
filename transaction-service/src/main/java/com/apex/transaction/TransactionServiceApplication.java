package com.apex.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
public class TransactionServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
        log.info(">>> Apex Banking Transaction Service initialized on port 8082 <<<");
    }
}
