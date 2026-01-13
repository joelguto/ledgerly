package com.ledgerly.config;

import com.ledgerly.engine.DataType;
import com.ledgerly.engine.LedgerEngine;
import com.ledgerly.engine.TableSchema;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class SeedConfig {

    private final LedgerEngine engine;
    private final String dataDir;
    private final boolean seedEnabled;

    public SeedConfig(LedgerEngine engine,
                      org.springframework.beans.factory.annotation.Value("${ledgerly.data-dir:data}") String dataDir,
                      org.springframework.beans.factory.annotation.Value("${ledgerly.seed.enabled:true}") boolean seedEnabled) {
        this.engine = engine;
        this.dataDir = dataDir;
        this.seedEnabled = seedEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        if (!seedEnabled || hasExistingData()) {
            return;
        }
        TableSchema customers = new TableSchema(
                "customers",
                List.of(
                        new com.ledgerly.engine.ColumnDefinition("id", DataType.INT, false),
                        new com.ledgerly.engine.ColumnDefinition("name", DataType.STRING, false),
                        new com.ledgerly.engine.ColumnDefinition("created_at", DataType.TIMESTAMP, false)
                ),
                List.of("id"),
                List.of());

        TableSchema orders = new TableSchema(
                "orders",
                List.of(
                        new com.ledgerly.engine.ColumnDefinition("id", DataType.INT, false),
                        new com.ledgerly.engine.ColumnDefinition("customer_id", DataType.INT, false),
                        new com.ledgerly.engine.ColumnDefinition("amount", DataType.INT, false),
                        new com.ledgerly.engine.ColumnDefinition("created_at", DataType.TIMESTAMP, false)
                ),
                List.of("id"),
                List.of());

        engine.createTable(customers);
        engine.createTable(orders);
        engine.insert("customers", Map.of("id", 1L, "name", "Alice", "created_at", "2024-01-01T00:00:00Z"));
        engine.insert("customers", Map.of("id", 2L, "name", "Bob", "created_at", "2024-01-02T00:00:00Z"));
        engine.insert("orders", Map.of("id", 100L, "customer_id", 1L, "amount", 2500L, "created_at", "2024-01-03T00:00:00Z"));
        engine.insert("orders", Map.of("id", 101L, "customer_id", 2L, "amount", 1500L, "created_at", "2024-01-04T00:00:00Z"));
        System.out.println("Seeded sample tables (customers, orders)");
    }

    private boolean hasExistingData() {
        Path wal = Path.of(dataDir).resolve("ledgerly-wal.jsonl");
        return Files.exists(wal);
    }
}
