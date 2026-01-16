package com.ledgerly.repl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerly.domain.DomainService;
import com.ledgerly.domain.TransactionState;
import com.ledgerly.engine.ColumnDefinition;
import com.ledgerly.engine.Condition;
import com.ledgerly.engine.ConditionOperator;
import com.ledgerly.engine.DataType;
import com.ledgerly.engine.LedgerEngine;
import com.ledgerly.engine.RowPredicate;
import com.ledgerly.engine.TableSchema;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        System.out.println("Ledgerly REPL. Commands: help, tables, describe <table>, create <table> <schemaJson>, insert <table> <json>, select <table> [cols] [filters], update <table> <filters> <json>, delete <table> <col>=<val>, join <left> <right> <leftCol> <rightCol> [projections], merchant:create <json>, tx:create <json>, tx:get <id>, tx:list, tx:outcome <id> <json>, tx:expire, quit");
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
            String[] head = splitOnce(line);
            String cmd = head[0].toLowerCase(Locale.ROOT);
            String rest = head[1];
            switch (cmd) {
                case "help" -> printHelp();
                case "tables" -> engine.listTables().forEach(System.out::println);
                case "describe" -> {
                    String table = requireToken(rest, "describe <table>");
                    engine.describe(table).ifPresentOrElse(
                            this::printJson,
                            () -> System.out.println("Table not found"));
                }
                case "create" -> {
                    String table = requireToken(rest, "create <table> <schemaJson>");
                    String schemaJson = requireRemainder(afterFirst(rest), "create <table> <schemaJson>");
                    Map<String, Object> schema = mapper.readValue(schemaJson, new TypeReference<>() {});
                    TableSchema tableSchema = toSchema(table, schema);
                    engine.createTable(tableSchema);
                    System.out.println("Created table: " + table);
                }
                case "insert" -> {
                    String table = requireToken(rest, "insert <table> <json>");
                    String json = requireRemainder(afterFirst(rest), "insert <table> <json>");
                    Map<String, Object> vals = mapper.readValue(json, new TypeReference<>() {});
                    engine.insert(table, vals);
                    System.out.println("OK");
                }
                case "select" -> {
                    SelectArgs args = parseSelect(rest);
                    var rows = engine.select(args.table(), args.columns(), args.predicate());
                    printJson(rows);
                }
                case "update" -> {
                    UpdateArgs args = parseUpdate(rest);
                    int updated = engine.update(args.table(), args.predicate(), args.newValues());
                    System.out.println("Updated: " + updated);
                }
                case "delete" -> {
                    String table = requireToken(rest, "delete <table> col=val");
                    String filter = requireRemainder(afterFirst(rest), "delete <table> col=val");
                    RowPredicate predicate = parseFilters(filter);
                    int deleted = engine.delete(table, predicate);
                    System.out.println("Deleted: " + deleted);
                }
                case "join" -> {
                    JoinArgs args = parseJoin(rest);
                    var rows = engine.join(args.left(), args.right(), args.leftCol(), args.rightCol(), args.projection());
                    printJson(rows);
                }
                case "merchant:create" -> {
                    String json = requireRemainder(rest, "merchant:create <json>");
                    Map<String, Object> m = mapper.readValue(json, new TypeReference<>() {});
                    domain.createMerchant(
                            (String) m.get("id"),
                            (String) m.get("name"),
                            (String) m.getOrDefault("status", "ACTIVE"));
                    System.out.println("OK");
                }
                case "tx:create" -> {
                    String json = requireRemainder(rest, "tx:create <json>");
                    Map<String, Object> m = mapper.readValue(json, new TypeReference<>() {});
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
                    String id = requireToken(rest, "tx:get <id>");
                    var tx = domain.getTransaction(id);
                    printJson(tx);
                }
                case "tx:list" -> {
                    var rows = domain.listTransactions(null, null);
                    printJson(rows);
                }
                case "tx:outcome" -> {
                    String txId = requireToken(rest, "tx:outcome <id> <json>");
                    String json = requireRemainder(afterFirst(rest), "tx:outcome <id> <json>");
                    Map<String, Object> m = mapper.readValue(json, new TypeReference<>() {});
                    TransactionState state = TransactionState.valueOf(((String) m.get("status")).toUpperCase());
                    domain.assertOutcome(
                            txId,
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

    private void printHelp() {
        System.out.println("""
                help
                  Show this help

                tables
                  List tables

                describe <table>
                  Show schema for a table
                  Example: describe customers

                create <table> <schemaJson>
                  Create table with columns, primaryKey, optional unique[]
                  Example:
                    create demo {"columns":[{"name":"id","type":"INT"},{"name":"name","type":"STRING"}],"primaryKey":["id"],"unique":[["name"]]}

                insert <table> <json>
                  Insert a row (JSON object)
                  Example: insert customers {"id":4,"name":"Diana","created_at":"2024-02-01T00:00:00Z"}

                select <table> [cols] [col=val,...]
                  Select rows with optional projection and equality filters
                  Examples:
                    select customers
                    select customers id,name
                    select customers id,name id=1,state=ACTIVE

                update <table> col=val,... <json>
                  Update rows matching equality filters
                  Example: update customers id=4 {"name":"Diana Updated"}

                delete <table> col=val
                  Delete rows matching equality filter
                  Example: delete customers id=4

                join <left> <right> <lCol> <rCol> [proj1,proj2]
                  Inner join with optional projection list
                  Example: join customers orders id customer_id customers.id,orders.amount

                merchant:create <json>
                  Create merchant (id,name,status)
                  Example: merchant:create {"id":"m3","name":"Corner Shop","status":"ACTIVE"}

                tx:create <json>
                  Create transaction
                  Example: tx:create {"id":"t300","merchant_id":"m3","amount":1200,"currency":"USD","expires_at":"2026-01-15T00:00:00Z"}

                tx:get <id>
                  Get transaction

                tx:list
                  List transactions

                tx:outcome <id> <json>
                  Assert outcome
                  Example: tx:outcome t300 {"status":"SUCCESS","external_reference":"proc-22"}

                tx:expire
                  Expire pending transactions

                quit
                  Exit
                """);
    }

    private TableSchema toSchema(String name, Map<String, Object> schema) {
        Object colsObj = schema.get("columns");
        if (!(colsObj instanceof List<?> colsList)) {
            throw new IllegalArgumentException("Schema must include columns[]");
        }
        List<ColumnDefinition> columns = new ArrayList<>();
        for (Object o : colsList) {
            if (!(o instanceof Map<?, ?> colMap)) {
                throw new IllegalArgumentException("Column entry must be an object");
            }
            String colName = Objects.toString(colMap.get("name"), null);
            if (colName == null || colName.isBlank()) {
                throw new IllegalArgumentException("Column name required");
            }
            String typeRaw = Objects.toString(colMap.get("type"), null);
            if (typeRaw == null) {
                throw new IllegalArgumentException("Column type required");
            }
            DataType type = DataType.valueOf(typeRaw.toUpperCase(Locale.ROOT));
            boolean nullable = Boolean.TRUE.equals(colMap.get("nullable"));
            columns.add(new ColumnDefinition(colName, type, nullable));
        }

        List<String> pk = readStringList(schema.get("primaryKey"), "primaryKey");
        List<List<String>> uniques = readNestedStringLists(schema.get("unique"));
        return new TableSchema(name, columns, pk, uniques);
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Object o, String field) {
        if (o == null) {
            throw new IllegalArgumentException(field + " required");
        }
        if (!(o instanceof List<?> list)) {
            throw new IllegalArgumentException(field + " must be a list");
        }
        return ((List<Object>) list).stream()
                .map(v -> Objects.toString(v, null))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<List<String>> readNestedStringLists(Object o) {
        if (o == null) {
            return List.of();
        }
        if (!(o instanceof List<?> outer)) {
            throw new IllegalArgumentException("unique must be a list of lists");
        }
        List<List<String>> uniques = new ArrayList<>();
        for (Object inner : outer) {
            if (!(inner instanceof List<?> innerList)) {
                throw new IllegalArgumentException("unique entries must be lists");
            }
            List<String> values = ((List<Object>) innerList).stream()
                    .map(v -> Objects.toString(v, null))
                    .collect(Collectors.toList());
            uniques.add(values);
        }
        return uniques;
    }

    private SelectArgs parseSelect(String rest) {
        String[] parts = rest.isEmpty() ? new String[]{} : rest.split("\\s+", 3);
        if (parts.length == 0) {
            throw new IllegalArgumentException("select <table> [cols] [col=val,...]");
        }
        String table = parts[0];
        List<String> cols = null;
        RowPredicate predicate = null;
        if (parts.length >= 2 && !parts[1].isBlank()) {
            cols = parseCsv(parts[1]);
        }
        if (parts.length == 3 && !parts[2].isBlank()) {
            predicate = parseFilters(parts[2]);
        }
        return new SelectArgs(table, cols, predicate);
    }

    private UpdateArgs parseUpdate(String rest) {
        String table = requireToken(rest, "update <table> col=val,... <json>");
        String afterTable = afterFirst(rest);
        String filters = requireToken(afterTable, "update <table> col=val,... <json>");
        String json = requireRemainder(afterFirst(afterTable), "update <table> col=val,... <json>");
        RowPredicate predicate = parseFilters(filters);
        Map<String, Object> vals = readJsonObject(json);
        return new UpdateArgs(table, predicate, vals);
    }

    private JoinArgs parseJoin(String rest) {
        String[] parts = rest.isEmpty() ? new String[]{} : rest.split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("join <left> <right> <leftCol> <rightCol> [proj1,proj2]");
        }
        String left = parts[0];
        String right = parts[1];
        String leftCol = parts[2];
        String rightCol = parts[3];
        List<String> projection = parts.length >= 5 ? parseCsv(parts[4]) : null;
        return new JoinArgs(left, right, leftCol, rightCol, projection);
    }

    private RowPredicate parseFilters(String expr) {
        if (expr == null || expr.isBlank()) {
            return null;
        }
        String[] pairs = expr.split(",");
        List<Condition> conditions = new ArrayList<>();
        for (String p : pairs) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) {
                throw new IllegalArgumentException("Filter must be col=val, multiple with commas");
            }
            conditions.add(new Condition(kv[0], ConditionOperator.EQ, parseValue(kv[1])));
        }
        return new RowPredicate(conditions);
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        String[] parts = csv.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) {
                out.add(p);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private Object parseValue(String raw) {
        if (raw == null) return null;
        if ("null".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private String[] splitOnce(String input) {
        int idx = input.indexOf(' ');
        if (idx < 0) {
            return new String[]{input, ""};
        }
        return new String[]{input.substring(0, idx), input.substring(idx + 1).trim()};
    }

    private String afterFirst(String input) {
        int idx = input.indexOf(' ');
        if (idx < 0) {
            return "";
        }
        return input.substring(idx + 1).trim();
    }

    private String requireToken(String text, String usage) {
        String[] parts = splitOnce(text);
        if (parts[0].isBlank()) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
        return parts[0];
    }

    private String requireRemainder(String text, String usage) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
        return text;
    }

    private Map<String, Object> readJsonObject(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON: " + ex.getMessage(), ex);
        }
    }

    private record SelectArgs(String table, List<String> columns, RowPredicate predicate) {}

    private record UpdateArgs(String table, RowPredicate predicate, Map<String, Object> newValues) {}

    private record JoinArgs(String left, String right, String leftCol, String rightCol, List<String> projection) {}

    private void printJson(Object value) {
        try {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        } catch (Exception ex) {
            System.out.println("Error serializing value: " + ex.getMessage());
        }
    }
}
