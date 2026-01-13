package com.ledgerly.engine.persistence;

import com.ledgerly.engine.RowPredicate;
import com.ledgerly.engine.TableSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record PersistenceEvent(EventType type,
                               String tableName,
                               TableSchema schema,
                               PredicateSpec predicate,
                               Map<String, Object> values) {

    public enum EventType {
        CREATE_TABLE,
        INSERT,
        UPDATE,
        DELETE
    }

    public static PersistenceEvent createTable(TableSchema schema) {
        return new PersistenceEvent(EventType.CREATE_TABLE, schema.getName(), schema, null, null);
    }

    public static PersistenceEvent insert(String table, Map<String, Object> values) {
        return new PersistenceEvent(EventType.INSERT, table, null, null, values);
    }

    public static PersistenceEvent update(String table, RowPredicate predicate, Map<String, Object> values) {
        return new PersistenceEvent(EventType.UPDATE, table, null, toSpec(predicate), values);
    }

    public static PersistenceEvent delete(String table, RowPredicate predicate) {
        return new PersistenceEvent(EventType.DELETE, table, null, toSpec(predicate), null);
    }

    private static PredicateSpec toSpec(RowPredicate predicate) {
        if (predicate == null) {
            return new PredicateSpec(List.of());
        }
        return new PredicateSpec(predicate.conditions().stream()
                .map(c -> new ConditionSpec(c.getColumn(), c.getOperator(), c.getValue()))
                .collect(Collectors.toList()));
    }
}
