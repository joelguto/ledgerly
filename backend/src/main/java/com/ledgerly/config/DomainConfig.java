package com.ledgerly.config;

import com.ledgerly.domain.DomainService;
import com.ledgerly.engine.LedgerEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public DomainService domainService(LedgerEngine engine) {
        return new DomainService(engine);
    }
}
