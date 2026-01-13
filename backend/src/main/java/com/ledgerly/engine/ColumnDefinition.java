package com.ledgerly.engine;

import java.util.Objects;

public class ColumnDefinition {
    private final String name;
    private final DataType type;
    private final boolean nullable;

    public ColumnDefinition(String name, DataType type, boolean nullable) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.nullable = nullable;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public boolean isNullable() {
        return nullable;
    }
}
