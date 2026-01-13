package com.ledgerly.api.dto;

import java.util.List;
import java.util.Map;

public class UpdateRequest {
    private Map<String, Object> values;
    private List<ConditionDto> filters = List.of();

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public List<ConditionDto> getFilters() {
        return filters;
    }

    public void setFilters(List<ConditionDto> filters) {
        this.filters = filters;
    }
}
