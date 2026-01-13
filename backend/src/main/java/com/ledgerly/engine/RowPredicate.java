package com.ledgerly.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RowPredicate {
    private final List<Condition> conditions;

    public RowPredicate(List<Condition> conditions) {
        this.conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
    }

    public List<Condition> conditions() {
        return conditions;
    }

    public boolean test(Map<String, Object> row) {
        for (Condition condition : conditions) {
            if (!evaluate(condition, row.get(condition.getColumn()))) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluate(Condition condition, Object value) {
        ConditionOperator op = condition.getOperator();
        Object rhs = condition.getValue();
        return switch (op) {
            case IS_NULL -> value == null;
            case IS_NOT_NULL -> value != null;
            case EQ -> compare(value, rhs) == 0;
            case NEQ -> compare(value, rhs) != 0;
            case LT -> compare(value, rhs) < 0;
            case LTE -> compare(value, rhs) <= 0;
            case GT -> compare(value, rhs) > 0;
            case GTE -> compare(value, rhs) >= 0;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object left, Object right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Cannot compare null values");
        }
        if (!(left instanceof Comparable<?> l) || !(right instanceof Comparable<?> r)) {
            throw new IllegalArgumentException("Values are not comparable");
        }
        return ((Comparable) l).compareTo(r);
    }
}
