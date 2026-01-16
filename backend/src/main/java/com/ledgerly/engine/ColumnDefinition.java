package com.ledgerly.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ColumnDefinition {
    private final String name;
    private final DataType type;
    private final boolean nullable;

    @JsonCreator
    public ColumnDefinition(@JsonProperty("name") String name,
                            @JsonProperty("type") DataType type,
                            @JsonProperty("nullable") boolean nullable) {
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
