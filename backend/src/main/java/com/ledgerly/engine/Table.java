package com.ledgerly.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Table {
    private final TableSchema schema;
    private final AtomicLong idSeq = new AtomicLong(1);
    private final Map<Long, RowRecord> rows = new LinkedHashMap<>();
    private final Map<String, Map<List<Object>, Long>> uniqueIndexes = new HashMap<>();

    public Table(TableSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
        // create index for primary key
        uniqueIndexes.put("pk", new HashMap<>());
        // unique constraints indexes
        int idx = 0;
        for (List<String> uniqueCols : schema.getUniqueConstraints()) {
            uniqueIndexes.put("u" + idx++, new HashMap<>());
        }
    }

    public TableSchema getSchema() {
        return schema;
    }

    public synchronized void insert(Map<String, Object> values) {
        Map<String, Object> coerced = coerceAndValidate(values, true);
        List<Object> pkKey = buildKey(schema.getPrimaryKey(), coerced);
        ensureUnique("pk", schema.getPrimaryKey(), pkKey, null);

        int uIndex = 0;
        for (List<String> unique : schema.getUniqueConstraints()) {
            List<Object> key = buildKey(unique, coerced);
            ensureUnique("u" + uIndex, unique, key, null);
            uIndex++;
        }

        long id = idSeq.getAndIncrement();
        RowRecord record = new RowRecord(id, coerced);
        rows.put(id, record);
        uniqueIndexes.get("pk").put(pkKey, id);

        uIndex = 0;
        for (List<String> unique : schema.getUniqueConstraints()) {
            List<Object> key = buildKey(unique, coerced);
            uniqueIndexes.get("u" + uIndex).put(key, id);
            uIndex++;
        }
    }

    public synchronized int update(RowPredicate predicate, Map<String, Object> newValues) {
        Map<String, Object> coercedUpdates = coerceAndValidate(newValues, false);
        List<Long> matched = findMatchingIds(predicate);

        int count = 0;
        for (Long id : matched) {
            RowRecord current = rows.get(id);
            Map<String, Object> merged = new HashMap<>(current.getValues());
            merged.putAll(coercedUpdates);
            validateNullability(merged);

            List<Object> pkKey = buildKey(schema.getPrimaryKey(), merged);
            ensureUnique("pk", schema.getPrimaryKey(), pkKey, id);

            int uIndex = 0;
            for (List<String> unique : schema.getUniqueConstraints()) {
                List<Object> key = buildKey(unique, merged);
                ensureUnique("u" + uIndex, unique, key, id);
                uIndex++;
            }

            RowRecord updated = new RowRecord(id, merged);
            rows.put(id, updated);
            uniqueIndexes.get("pk").put(pkKey, id);

            uIndex = 0;
            for (List<String> unique : schema.getUniqueConstraints()) {
                List<Object> key = buildKey(unique, merged);
                uniqueIndexes.get("u" + uIndex).put(key, id);
                uIndex++;
            }
            count++;
        }
        return count;
    }

    public synchronized int delete(RowPredicate predicate) {
        List<Long> matched = findMatchingIds(predicate);
        for (Long id : matched) {
            RowRecord rec = rows.remove(id);
            List<Object> pkKey = buildKey(schema.getPrimaryKey(), rec.getValues());
            uniqueIndexes.get("pk").remove(pkKey);
            int uIndex = 0;
            for (List<String> unique : schema.getUniqueConstraints()) {
                List<Object> key = buildKey(unique, rec.getValues());
                uniqueIndexes.get("u" + uIndex).remove(key);
                uIndex++;
            }
        }
        return matched.size();
    }

    public synchronized List<Map<String, Object>> select(List<String> columns, RowPredicate predicate) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RowRecord record : rows.values()) {
            if (predicate == null || predicate.test(record.getValues())) {
                if (columns == null || columns.isEmpty()) {
                    result.add(Collections.unmodifiableMap(record.getValues()));
                } else {
                    Map<String, Object> projected = new LinkedHashMap<>();
                    for (String col : columns) {
                        projected.put(col, record.getValues().get(col));
                    }
                    result.add(projected);
                }
            }
        }
        return result;
    }

    public synchronized List<RowRecord> rowsSnapshot() {
        return new ArrayList<>(rows.values());
    }

    private Map<String, Object> coerceAndValidate(Map<String, Object> values, boolean requireAllColumns) {
        Map<String, Object> out = new HashMap<>();
        for (String provided : values.keySet()) {
            if (!schema.getColumns().containsKey(provided)) {
                throw new IllegalArgumentException("Unknown column: " + provided);
            }
        }
        for (Map.Entry<String, ColumnDefinition> entry : schema.getColumns().entrySet()) {
            String colName = entry.getKey();
            ColumnDefinition colDef = entry.getValue();
            Object raw = values.get(colName);
            if (raw == null) {
                if (requireAllColumns && !colDef.isNullable()) {
                    throw new IllegalArgumentException("Column " + colName + " is required");
                }
                out.put(colName, null);
                continue;
            }
            Object coerced = coerceValue(colDef.getType(), raw);
            out.put(colName, coerced);
        }
        if (requireAllColumns) {
            validateNullability(out);
        }
        return out;
    }

    private Object coerceValue(DataType type, Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return type.parse(s);
        }
        return raw;
    }

    private void validateNullability(Map<String, Object> values) {
        for (Map.Entry<String, ColumnDefinition> entry : schema.getColumns().entrySet()) {
            if (!entry.getValue().isNullable() && values.get(entry.getKey()) == null) {
                throw new IllegalArgumentException("Column " + entry.getKey() + " cannot be null");
            }
        }
    }

    private List<Object> buildKey(List<String> cols, Map<String, Object> values) {
        List<Object> key = new ArrayList<>(cols.size());
        for (String col : cols) {
            key.add(values.get(col));
        }
        return key;
    }

    private void ensureUnique(String indexName, List<String> cols, List<Object> key, Long selfId) {
        Map<List<Object>, Long> index = uniqueIndexes.get(indexName);
        Long existingId = index.get(key);
        if (existingId != null && (selfId == null || !existingId.equals(selfId))) {
            throw new IllegalArgumentException("Unique constraint violation on " + cols);
        }
    }

    private List<Long> findMatchingIds(RowPredicate predicate) {
        List<Long> ids = new ArrayList<>();
        for (RowRecord record : rows.values()) {
            if (predicate == null || predicate.test(record.getValues())) {
                ids.add(record.getId());
            }
        }
        return ids;
    }
}
