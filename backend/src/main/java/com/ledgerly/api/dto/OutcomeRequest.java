package com.ledgerly.api.dto;

public class OutcomeRequest {
    private String status;
    private String externalReference;
    private String reportedAt;
    private String metadata;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(String reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
