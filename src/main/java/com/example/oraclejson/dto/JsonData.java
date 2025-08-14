package com.example.oraclejson.dto;

import java.time.LocalDateTime;

public class JsonData {
    private final long id;
    private final String messageKey;
    private final String jsonData;
    private final LocalDateTime createdAt;

    public JsonData(long id, String messageKey, String jsonData, LocalDateTime createdAt) {
        this.id = id;
        this.messageKey = messageKey;
        this.jsonData = jsonData;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getJsonData() {
        return jsonData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
