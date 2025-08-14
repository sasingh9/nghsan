package com.example.oraclejson.controller;

import com.example.oraclejson.DatabaseStorageService;
import com.example.oraclejson.dto.JsonData;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/data")
public class JsonDataController {

    private final DatabaseStorageService storageService;

    public JsonDataController(DatabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public List<JsonData> getData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // For simplicity, if dates are null, we can define a default behavior,
        // like fetching data from the last 24 hours or not applying the filter.
        // Here, we'll just pass them to the service layer.
        return storageService.getDataByDateRange(startDate, endDate);
    }
}
