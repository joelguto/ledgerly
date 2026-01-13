package com.ledgerly.api.dto;

import java.util.List;

public class QueryRequest {
    private List<String> columns = List.of();
    private List<ConditionDto> filters = List.of();

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<ConditionDto> getFilters() {
        return filters;
    }

    public void setFilters(List<ConditionDto> filters) {
        this.filters = filters;
    }
}
