package com.manacommunity.sports;

import com.manacommunity.common.enums.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableAsync
@EnableScheduling
@EntityScan(basePackages = "com.manacommunity")
@SpringBootApplication(scanBasePackages = {
        "com.manacommunity.sports"
})
public class ManaCommunityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManaCommunityServiceApplication.class, args);
    }
}

