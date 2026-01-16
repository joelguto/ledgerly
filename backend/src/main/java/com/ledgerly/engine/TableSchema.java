package com.ledgerly.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        Built built = build(name, toMap(columns), primaryKey, uniqueConstraints);
        this.name = built.name();
        this.columns = built.columns();
        this.primaryKey = built.primaryKey();
        this.uniqueConstraints = built.uniqueConstraints();
    }

    @JsonCreator
    public TableSchema(@JsonProperty("name") String name,
                       @JsonProperty("columns") Map<String, ColumnDefinition> columnsMap,
                       @JsonProperty("primaryKey") List<String> primaryKey,
                       @JsonProperty("uniqueConstraints") List<List<String>> uniqueConstraints) {
        Built built = build(name, columnsMap, primaryKey, uniqueConstraints);
        this.name = built.name();
        this.columns = built.columns();
        this.primaryKey = built.primaryKey();
        this.uniqueConstraints = built.uniqueConstraints();
    }

    private static Map<String, ColumnDefinition> toMap(List<ColumnDefinition> columns) {
        Objects.requireNonNull(columns, "columns");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }
        Map<String, ColumnDefinition> map = new LinkedHashMap<>();
        for (ColumnDefinition col : columns) {
            if (map.containsKey(col.getName())) {
                throw new IllegalArgumentException("Duplicate column: " + col.getName());
            }
            map.put(col.getName(), col);
        }
        return map;
    }

    private record Built(String name,
                         Map<String, ColumnDefinition> columns,
                         List<String> primaryKey,
                         List<List<String>> uniqueConstraints) {}

    private static Built build(String name,
                               Map<String, ColumnDefinition> columnsMap,
                               List<String> primaryKey,
                               List<List<String>> uniqueConstraints) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(columnsMap, "columns");
        if (columnsMap.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }
        Map<String, ColumnDefinition> map = new LinkedHashMap<>();
        for (Map.Entry<String, ColumnDefinition> entry : columnsMap.entrySet()) {
            String key = entry.getKey();
            ColumnDefinition col = entry.getValue();
            if (map.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate column: " + key);
            }
            map.put(key, col);
        }

        Objects.requireNonNull(primaryKey, "primaryKey");
        if (primaryKey.isEmpty()) {
            throw new IllegalArgumentException("Primary key required");
        }
        for (String col : primaryKey) {
            if (!map.containsKey(col)) {
                throw new IllegalArgumentException("Primary key references missing column: " + col);
            }
        }
        List<String> pk = List.copyOf(primaryKey);

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
        List<List<String>> uniques = uniqueConstraints.stream()
                .map(List::copyOf)
                .collect(Collectors.toUnmodifiableList());

        return new Built(name, Collections.unmodifiableMap(map), pk, uniques);
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
