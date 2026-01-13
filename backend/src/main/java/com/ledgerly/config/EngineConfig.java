package com.ledgerly.config;

import com.ledgerly.engine.LedgerEngine;
import com.ledgerly.engine.persistence.FilePersistence;
import com.ledgerly.engine.persistence.Persistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class EngineConfig {

    @Bean
    public Persistence persistence(@Value("${ledgerly.data-dir:data}") String dataDir) {
        return new FilePersistence(Path.of(dataDir));
    }

    @Bean
    public LedgerEngine ledgerEngine(Persistence persistence) {
        return new LedgerEngine(persistence);
    }
}
