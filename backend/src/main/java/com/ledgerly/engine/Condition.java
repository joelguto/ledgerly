package com.ledgerly.engine;

import java.util.Objects;

public class Condition {
    private final String column;
    private final ConditionOperator operator;
    private final Object value;

    public Condition(String column, ConditionOperator operator, Object value) {
        this.column = Objects.requireNonNull(column, "column");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }
}
