package com.ledgerly.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class LedgerEngine {

    private final Map<String, Table> tables = new HashMap<>();
    private final Persistence persistence;

    public LedgerEngine(Persistence persistence) {
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        for (PersistenceEvent event : persistence.loadEvents()) {
            applyEvent(event, false);
        }
    }

    public synchronized void createTable(TableSchema schema) {
        String name = schema.getName();
        if (tables.containsKey(name)) {
            throw new IllegalArgumentException("Table already exists: " + name);
        }
        Table table = new Table(schema);
        tables.put(name, table);
        persistence.appendEvent(PersistenceEvent.createTable(schema));
    }

    public synchronized void insert(String tableName, Map<String, Object> values) {
        Table table = getTable(tableName);
        table.insert(values);
        persistence.appendEvent(PersistenceEvent.insert(tableName, values));
    }

    public synchronized int update(String tableName, RowPredicate predicate, Map<String, Object> newValues) {
        Table table = getTable(tableName);
        int count = table.update(predicate, newValues);
        if (count > 0) {
            persistence.appendEvent(PersistenceEvent.update(tableName, predicate, newValues));
        }
        return count;
    }

    public synchronized int delete(String tableName, RowPredicate predicate) {
        Table table = getTable(tableName);
        int count = table.delete(predicate);
        if (count > 0) {
            persistence.appendEvent(PersistenceEvent.delete(tableName, predicate));
        }
        return count;
    }

    public synchronized List<Map<String, Object>> select(String tableName, List<String> columns, RowPredicate predicate) {
        Table table = getTable(tableName);
        return table.select(columns, predicate);
    }

    public synchronized List<Map<String, Object>> join(String leftTableName,
                                                       String rightTableName,
                                                       String leftColumn,
                                                       String rightColumn,
                                                       List<String> projection) {
        Table left = getTable(leftTableName);
        Table right = getTable(rightTableName);

        List<Map<String, Object>> results = new ArrayList<>();
        for (RowRecord leftRow : left.rowsSnapshot()) {
            Object leftKey = leftRow.getValues().get(leftColumn);
            for (RowRecord rightRow : right.rowsSnapshot()) {
                Object rightKey = rightRow.getValues().get(rightColumn);
                if (Objects.equals(leftKey, rightKey)) {
                    Map<String, Object> merged = new HashMap<>();
                    merged.putAll(prefixMap(leftTableName, leftRow.getValues()));
                    merged.putAll(prefixMap(rightTableName, rightRow.getValues()));
                    if (projection != null && !projection.isEmpty()) {
                        Map<String, Object> filtered = new HashMap<>();
                        for (String p : projection) {
                            filtered.put(p, merged.get(p));
                        }
                        results.add(filtered);
                    } else {
                        results.add(merged);
                    }
                }
            }
        }
        return results;
    }

    public synchronized Optional<TableSchema> describe(String tableName) {
        Table table = tables.get(tableName);
        return table == null ? Optional.empty() : Optional.of(table.getSchema());
    }

    public synchronized List<String> listTables() {
        return new ArrayList<>(tables.keySet());
    }

    Table getTable(String name) {
        Table table = tables.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + name);
        }
        return table;
    }

    Map<String, Table> tablesView() {
        return tables;
    }

    private Map<String, Object> prefixMap(String tableName, Map<String, Object> values) {
        Map<String, Object> out = new HashMap<>();
        values.forEach((k, v) -> out.put(tableName + "." + k, v));
        return out;
    }

    private void applyEvent(PersistenceEvent event, boolean log) {
        RowPredicate predicate = toPredicate(event.predicate());
        switch (event.type()) {
            case CREATE_TABLE -> applyCreate(event.schema());
            case INSERT -> applyInsert(event.tableName(), event.values());
            case UPDATE -> applyUpdate(event.tableName(), predicate, event.values());
            case DELETE -> applyDelete(event.tableName(), predicate);
        }
        if (log) {
            persistence.appendEvent(event);
        }
    }

    private void applyCreate(TableSchema schema) {
        tables.put(schema.getName(), new Table(schema));
    }

    private void applyInsert(String tableName, Map<String, Object> values) {
        getTable(tableName).insert(values);
    }

    private void applyUpdate(String tableName, RowPredicate predicate, Map<String, Object> newValues) {
        getTable(tableName).update(predicate, newValues);
    }

    private void applyDelete(String tableName, RowPredicate predicate) {
        getTable(tableName).delete(predicate);
    }

    private RowPredicate toPredicate(com.ledgerly.engine.persistence.PredicateSpec spec) {
        if (spec == null || spec.conditions() == null || spec.conditions().isEmpty()) {
            return null;
        }
        List<Condition> conditions = spec.conditions().stream()
                .map(c -> new Condition(c.column(), c.operator(), c.value()))
                .toList();
        return new RowPredicate(conditions);
    }
}
