package com.manacommunity.sports.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
        "com.manacommunity.sports.model",
        "com.manacommunity.common.model",
        "com.manacommunity.common.user.model"
})
@EnableJpaRepositories(basePackages = {
        "com.manacommunity.sports.repository",
        "com.manacommunity.common.repository",
        "com.manacommunity.common.user.repository"
})
public class SportsJpaConfig {
}
