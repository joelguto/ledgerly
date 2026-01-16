package com.ledgerly.domain;

import com.ledgerly.engine.ColumnDefinition;
import com.ledgerly.engine.DataType;
import com.ledgerly.engine.LedgerEngine;
import com.ledgerly.engine.TableSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class DomainSchemaInitializer {

    private final LedgerEngine engine;
    private final boolean seedDomain;

    public DomainSchemaInitializer(LedgerEngine engine,
                                   @Value("${ledgerly.seed.domain-enabled:true}") boolean seedDomain) {
        this.engine = engine;
        this.seedDomain = seedDomain;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSchemas() {
        ensureMerchants();
        ensureTransactions();
        ensureOutcomes();
        if (seedDomain) {
            seed();
        }
    }

    private void ensureMerchants() {
        if (engine.describe("merchants").isPresent()) return;
        TableSchema schema = new TableSchema(
                "merchants",
                List.of(
                        new ColumnDefinition("id", DataType.STRING, false),
                        new ColumnDefinition("name", DataType.STRING, false),
                        new ColumnDefinition("status", DataType.STRING, false),
                        new ColumnDefinition("created_at", DataType.TIMESTAMP, false)
                ),
                List.of("id"),
                List.of()
        );
        engine.createTable(schema);
    }

    private void ensureTransactions() {
        if (engine.describe("transactions").isPresent()) return;
        TableSchema schema = new TableSchema(
                "transactions",
                List.of(
                        new ColumnDefinition("id", DataType.STRING, false),
                        new ColumnDefinition("merchant_id", DataType.STRING, false),
                        new ColumnDefinition("amount", DataType.INT, false),
                        new ColumnDefinition("currency", DataType.STRING, false),
                        new ColumnDefinition("state", DataType.STRING, false),
                        new ColumnDefinition("created_at", DataType.TIMESTAMP, false),
                        new ColumnDefinition("expires_at", DataType.TIMESTAMP, false),
                        new ColumnDefinition("metadata", DataType.STRING, true)
                ),
                List.of("id"),
                List.of()
        );
        engine.createTable(schema);
    }

    private void ensureOutcomes() {
        if (engine.describe("outcomes").isPresent()) return;
        TableSchema schema = new TableSchema(
                "outcomes",
                List.of(
                        new ColumnDefinition("tx_id", DataType.STRING, false),
                        new ColumnDefinition("status", DataType.STRING, false),
                        new ColumnDefinition("external_reference", DataType.STRING, true),
                        new ColumnDefinition("reported_at", DataType.TIMESTAMP, false),
                        new ColumnDefinition("metadata", DataType.STRING, true)
                ),
                List.of("tx_id"),
                List.of()
        );
        engine.createTable(schema);
    }

    private void seed() {
        if (!isEmpty("merchants")) {
            return;
        }
        String now = Instant.now().toString();
        engine.insert("merchants", Map.of(
                "id", "1",
                "name", "joelguto",
                "status", "ACTIVE",
                "created_at", now
        ));

        // Pre-seeded successful transactions (KES)
        insertTxWithOutcome("t201", "1", 12500L, "KES", now, "seeded: pos", "ref-201");
        insertTxWithOutcome("t202", "1", 8800L, "KES", now, "seeded: web", "ref-202");
        insertTxWithOutcome("t203", "1", 4500L, "KES", now, "seeded: ussd", "ref-203");
        insertTxWithOutcome("t204", "1", 17999L, "KES", now, "seeded: mobile", "ref-204");
        insertTxWithOutcome("t205", "1", 2999L, "KES", now, "seeded: retry", "ref-205");
    }

    private boolean isEmpty(String table) {
        return engine.select(table, List.of(), null).isEmpty();
    }

    private void insertTxWithOutcome(String id,
                                     String merchantId,
                                     long amount,
                                     String currency,
                                     String createdAt,
                                     String metadata,
                                     String externalRef) {
        engine.insert("transactions", Map.of(
                "id", id,
                "merchant_id", merchantId,
                "amount", amount,
                "currency", currency,
                "state", TransactionState.SUCCESS.name(),
                "created_at", createdAt,
                "expires_at", Instant.now().plusSeconds(3600).toString(),
                "metadata", metadata
        ));
        engine.insert("outcomes", Map.of(
                "tx_id", id,
                "status", TransactionState.SUCCESS.name(),
                "external_reference", externalRef,
                "reported_at", createdAt,
                "metadata", metadata
        ));
    }
}
