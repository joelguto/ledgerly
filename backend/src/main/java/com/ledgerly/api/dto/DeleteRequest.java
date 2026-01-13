package com.ledgerly.api.dto;

import java.util.List;

public class DeleteRequest {
    private List<ConditionDto> filters = List.of();

    public List<ConditionDto> getFilters() {
        return filters;
    }

    public void setFilters(List<ConditionDto> filters) {
        this.filters = filters;
    }
}
