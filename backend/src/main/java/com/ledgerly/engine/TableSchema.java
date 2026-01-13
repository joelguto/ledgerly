package com.ledgerly.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSchema {
    private final String name;
    private final Map<String, ColumnDefinition> columns; // preserves order
    private final List<String> primaryKey;
    private final List<List<String>> uniqueConstraints;

    public TableSchema(String name,
                       List<ColumnDefinition> columns,
                       List<String> primaryKey,
                       List<List<String>> uniqueConstraints) {
        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(columns, "columns");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }
        Map<String, ColumnDefinition> map = new LinkedHashMap<>();
        for (ColumnDefinition col : columns) {
            String key = col.getName();
            if (map.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate column: " + key);
            }
            map.put(key, col);
        }
        this.columns = Collections.unmodifiableMap(map);

        Objects.requireNonNull(primaryKey, "primaryKey");
        if (primaryKey.isEmpty()) {
            throw new IllegalArgumentException("Primary key required");
        }
        for (String col : primaryKey) {
            if (!map.containsKey(col)) {
                throw new IllegalArgumentException("Primary key references missing column: " + col);
            }
        }
        this.primaryKey = List.copyOf(primaryKey);

        Objects.requireNonNull(uniqueConstraints, "uniqueConstraints");
        for (List<String> u : uniqueConstraints) {
            if (u.isEmpty()) {
                throw new IllegalArgumentException("Unique constraint cannot be empty");
            }
            for (String col : u) {
                if (!map.containsKey(col)) {
                    throw new IllegalArgumentException("Unique constraint references missing column: " + col);
                }
            }
        }
        this.uniqueConstraints = uniqueConstraints.stream()
                .map(List::copyOf)
                .collect(Collectors.toUnmodifiableList());
    }

    public String getName() {
        return name;
    }

    public Map<String, ColumnDefinition> getColumns() {
        return columns;
    }

    public List<String> getPrimaryKey() {
        return primaryKey;
    }

    public List<List<String>> getUniqueConstraints() {
        return uniqueConstraints;
    }

    public Set<String> columnNames() {
        return columns.keySet();
    }
}
