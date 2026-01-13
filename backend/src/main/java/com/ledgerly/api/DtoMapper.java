package com.ledgerly.api;

import com.ledgerly.api.dto.ColumnDto;
import com.ledgerly.api.dto.ConditionDto;
import com.ledgerly.engine.ColumnDefinition;
import com.ledgerly.engine.Condition;
import com.ledgerly.engine.ConditionOperator;
import com.ledgerly.engine.DataType;
import com.ledgerly.engine.RowPredicate;
import com.ledgerly.engine.TableSchema;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class DtoMapper {
    private DtoMapper() {}

    public static TableSchema toSchema(String name,
                                       List<ColumnDto> columns,
                                       List<String> primaryKey,
                                       List<List<String>> uniques) {
        List<ColumnDefinition> defs = columns.stream()
                .map(DtoMapper::toColumn)
                .toList();
        return new TableSchema(name, defs, primaryKey, uniques);
    }

    public static RowPredicate toPredicate(List<ConditionDto> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        List<Condition> conditions = filters.stream()
                .map(f -> new Condition(f.getColumn(), toOperator(f.getOperator()), f.getValue()))
                .collect(Collectors.toList());
        return new RowPredicate(conditions);
    }

    private static ColumnDefinition toColumn(ColumnDto dto) {
        return new ColumnDefinition(dto.getName(), toType(dto.getType()), dto.isNullable());
    }

    private static DataType toType(String raw) {
        try {
            return DataType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported type: " + raw);
        }
    }

    private static ConditionOperator toOperator(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Operator is required");
        }
        return ConditionOperator.valueOf(raw.toUpperCase(Locale.ROOT));
    }
}
