package com.example.oraclejson.dto;

import java.time.LocalDateTime;

public class TradeExceptionData {
    private long id;
    private String clientReferenceNumber;
    private String failedTradeJson;
    private String failureReason;
    private LocalDateTime createdAt;

    public TradeExceptionData() {
    }

    public TradeExceptionData(long id, String clientReferenceNumber, String failedTradeJson, String failureReason, LocalDateTime createdAt) {
        this.id = id;
        this.clientReferenceNumber = clientReferenceNumber;
        this.failedTradeJson = failedTradeJson;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClientReferenceNumber() {
        return clientReferenceNumber;
    }

    public void setClientReferenceNumber(String clientReferenceNumber) {
        this.clientReferenceNumber = clientReferenceNumber;
    }

    public String getFailedTradeJson() {
        return failedTradeJson;
    }

    public void setFailedTradeJson(String failedTradeJson) {
        this.failedTradeJson = failedTradeJson;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
