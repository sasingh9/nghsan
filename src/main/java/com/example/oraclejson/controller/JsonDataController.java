package com.example.oraclejson.controller;

import com.example.oraclejson.DatabaseStorageService;
import com.example.oraclejson.dto.JsonData;
import com.example.oraclejson.dto.TradeDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class JsonDataController {

    private final DatabaseStorageService storageService;

    public JsonDataController(DatabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/data")
    public List<JsonData> getData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // For simplicity, if dates are null, we can define a default behavior,
        // like fetching data from the last 24 hours or not applying the filter.
        // Here, we'll just pass them to the service layer.
        return storageService.getDataByDateRange(startDate, endDate);
    }

    @GetMapping("/trades/{clientReferenceNumber}")
    public List<TradeDetails> getTradesByClientReference(@PathVariable String clientReferenceNumber) {
        return storageService.getTradeDetailsByClientReference(clientReferenceNumber);
    }

    /**
     * A temporary endpoint for manually posting data for testing purposes.
     * In a real application, this data would come from a Kafka topic.
     */
    @PostMapping("/data")
    public ResponseEntity<String> receiveData(@RequestBody String jsonData) {
        try {
            String messageKey = storageService.save(jsonData);
            if (messageKey != null) {
                return ResponseEntity.ok("Data saved successfully with key: " + messageKey);
            } else {
                return ResponseEntity.badRequest().body("Could not save empty or null data.");
            }
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.status(500).body("Error saving data: " + e.getMessage());
        }
    }
}
