package com.apex.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AccountServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
        log.info(">>> Apex Banking Account Service initialized on port 8081 <<<");
    }
}
