package com.ledgerly.api.dto;

import java.util.List;

public class JoinRequest {
    private String leftTable;
    private String rightTable;
    private String leftColumn;
    private String rightColumn;
    private List<String> projection = List.of();

    public String getLeftTable() {
        return leftTable;
    }

    public void setLeftTable(String leftTable) {
        this.leftTable = leftTable;
    }

    public String getRightTable() {
        return rightTable;
    }

    public void setRightTable(String rightTable) {
        this.rightTable = rightTable;
    }

    public String getLeftColumn() {
        return leftColumn;
    }

    public void setLeftColumn(String leftColumn) {
        this.leftColumn = leftColumn;
    }

    public String getRightColumn() {
        return rightColumn;
    }

    public void setRightColumn(String rightColumn) {
        this.rightColumn = rightColumn;
    }

    public List<String> getProjection() {
        return projection;
    }

    public void setProjection(List<String> projection) {
        this.projection = projection;
    }
}
