package com.ledgerly.repl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerly.domain.DomainService;
import com.ledgerly.domain.TransactionState;
import com.ledgerly.engine.Condition;
import com.ledgerly.engine.ConditionOperator;
import com.ledgerly.engine.LedgerEngine;
import com.ledgerly.engine.RowPredicate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "ledgerly.repl.enabled", havingValue = "true")
public class ReplRunner implements CommandLineRunner {

    private final LedgerEngine engine;
    private final DomainService domain;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReplRunner(LedgerEngine engine, DomainService domain) {
        this.engine = engine;
        this.domain = domain;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Ledgerly REPL. Commands: help, tables, describe <table>, insert <table> <json>, select <table>, delete <table> <col>=<val>, merchant:create <json>, tx:create <json>, tx:get <id>, tx:list, tx:outcome <id> <json>, tx:expire, quit");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                handle(line);
            }
        }
    }

    private void handle(String line) {
        try {
            String[] parts = line.split("\\s+", 3);
            String cmd = parts[0].toLowerCase(Locale.ROOT);
            switch (cmd) {
                case "help" -> printHelp();
                case "tables" -> engine.listTables().forEach(System.out::println);
                case "describe" -> {
                    ensureArgs(parts, 2);
                    engine.describe(parts[1]).ifPresentOrElse(
                            s -> System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s)),
                            () -> System.out.println("Table not found"));
                }
                case "insert" -> {
                    ensureArgs(parts, 3);
                    Map<String, Object> vals = mapper.readValue(parts[2], new TypeReference<>() {});
                    engine.insert(parts[1], vals);
                    System.out.println("OK");
                }
                case "select" -> {
                    ensureArgs(parts, 2);
                    var rows = engine.select(parts[1], null, null);
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows));
                }
                case "delete" -> {
                    ensureArgs(parts, 3);
                    RowPredicate predicate = parseEqPredicate(parts[2]);
                    int deleted = engine.delete(parts[1], predicate);
                    System.out.println("Deleted: " + deleted);
                }
                case "merchant:create" -> {
                    ensureArgs(parts, 3);
                    Map<String, Object> m = mapper.readValue(parts[2], new TypeReference<>() {});
                    domain.createMerchant(
                            (String) m.get("id"),
                            (String) m.get("name"),
                            (String) m.getOrDefault("status", "ACTIVE"));
                    System.out.println("OK");
                }
                case "tx:create" -> {
                    ensureArgs(parts, 3);
                    Map<String, Object> m = mapper.readValue(parts[2], new TypeReference<>() {});
                    domain.createTransaction(
                            (String) m.get("id"),
                            (String) m.get("merchant_id"),
                            ((Number) m.get("amount")).longValue(),
                            (String) m.get("currency"),
                            m.containsKey("expires_at") ? java.time.Instant.parse((String) m.get("expires_at")) : null,
                            (String) m.get("metadata"));
                    System.out.println("OK");
                }
                case "tx:get" -> {
                    ensureArgs(parts, 2);
                    var tx = domain.getTransaction(parts[1]);
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tx));
                }
                case "tx:list" -> {
                    var rows = domain.listTransactions(null, null);
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows));
                }
                case "tx:outcome" -> {
                    ensureArgs(parts, 3);
                    Map<String, Object> m = mapper.readValue(parts[2], new TypeReference<>() {});
                    TransactionState state = TransactionState.valueOf(((String) m.get("status")).toUpperCase());
                    domain.assertOutcome(
                            parts[1],
                            state,
                            (String) m.get("external_reference"),
                            m.containsKey("reported_at") ? java.time.Instant.parse((String) m.get("reported_at")) : null,
                            (String) m.get("metadata"));
                    System.out.println("OK");
                }
                case "tx:expire" -> {
                    int expired = domain.expirePending();
                    System.out.println("Expired: " + expired);
                }
                default -> System.out.println("Unknown command");
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private RowPredicate parseEqPredicate(String expr) {
        String[] kv = expr.split("=", 2);
        if (kv.length != 2) {
            throw new IllegalArgumentException("Predicate must be col=value");
        }
        Condition cond = new Condition(kv[0], ConditionOperator.EQ, kv[1]);
        return new RowPredicate(List.of(cond));
    }

    private void ensureArgs(String[] parts, int min) {
        if (parts.length < min) {
            throw new IllegalArgumentException("Missing arguments");
        }
    }

    private void printHelp() {
        System.out.println("""
                help                         Show this help
                tables                       List tables
                describe <table>             Show schema
                insert <table> <json>        Insert row (JSON object)
                select <table>               Select all rows
                delete <table> col=val       Delete rows matching equality
                merchant:create <json>       Create merchant (id,name,status)
                tx:create <json>             Create transaction (id,merchant_id,amount,currency,expires_at?)
                tx:get <id>                  Get transaction
                tx:list                      List transactions
                tx:outcome <id> <json>       Assert outcome (status,external_reference?,reported_at?,metadata?)
                tx:expire                    Expire pending transactions
                quit                         Exit
                """);
    }
}
