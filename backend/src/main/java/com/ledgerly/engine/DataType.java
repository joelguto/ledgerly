package com.ledgerly.engine;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public enum DataType {
    INT,
    STRING,
    TIMESTAMP;

    public Object parse(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (this) {
            case INT -> Long.parseLong(raw);
            case STRING -> raw;
            case TIMESTAMP -> parseTimestamp(raw);
        };
    }

    private long parseTimestamp(String raw) {
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid TIMESTAMP: " + raw, ex);
        }
    }
}
