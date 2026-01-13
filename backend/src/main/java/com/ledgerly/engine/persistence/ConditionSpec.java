package com.ledgerly.engine.persistence;

import com.ledgerly.engine.ConditionOperator;

public record ConditionSpec(String column, ConditionOperator operator, Object value) {
}
