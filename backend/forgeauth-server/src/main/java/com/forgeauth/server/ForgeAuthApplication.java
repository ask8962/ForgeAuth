package com.forgeauth.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.forgeauth")
@EntityScan(basePackages = "com.forgeauth.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.forgeauth.infrastructure.persistence.adapter")
public class ForgeAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForgeAuthApplication.class, args);
    }
}
