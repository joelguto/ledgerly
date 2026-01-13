package com.ledgerly.api.dto;

import java.util.List;

public class CreateTableRequest {
    private String name;
    private List<ColumnDto> columns;
    private List<String> primaryKey;
    private List<List<String>> uniqueConstraints = List.of();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ColumnDto> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnDto> columns) {
        this.columns = columns;
    }

    public List<String> getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(List<String> primaryKey) {
        this.primaryKey = primaryKey;
    }

    public List<List<String>> getUniqueConstraints() {
        return uniqueConstraints;
    }

    public void setUniqueConstraints(List<List<String>> uniqueConstraints) {
        this.uniqueConstraints = uniqueConstraints;
    }
}
