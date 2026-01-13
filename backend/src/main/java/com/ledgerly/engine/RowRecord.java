package com.ledgerly.engine;

import java.util.Map;

public class RowRecord {
    private final long id;
    private final Map<String, Object> values;

    public RowRecord(long id, Map<String, Object> values) {
        this.id = id;
        this.values = values;
    }

    public long getId() {
        return id;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
