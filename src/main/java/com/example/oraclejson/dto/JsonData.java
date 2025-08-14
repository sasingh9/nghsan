package com.example.oraclejson.dto;

import java.time.LocalDateTime;

public class JsonData {
    private final long id;
    private final String jsonData;
    private final LocalDateTime createdAt;

    public JsonData(long id, String jsonData, LocalDateTime createdAt) {
        this.id = id;
        this.jsonData = jsonData;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getJsonData() {
        return jsonData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
