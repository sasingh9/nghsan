package com.example.oraclejson.controller;

import com.example.oraclejson.DatabaseStorageService;
import com.example.oraclejson.dto.JsonData;
import com.example.oraclejson.dto.TradeDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.example.oraclejson.dto.TradeExceptionData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
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
    public Page<JsonData> getData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Principal principal) {

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date.");
            }
            long daysBetween = java.time.Duration.between(startDate, endDate).toDays();
            if (daysBetween > 31) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The date range cannot exceed 31 days.");
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        return storageService.getDataByDateRangeForUser(startDate, endDate, principal.getName(), pageable);
    }

    @GetMapping("/trades/{clientReferenceNumber}")
    public List<TradeDetails> getTradesByClientReference(@PathVariable String clientReferenceNumber, Principal principal) {
        return storageService.getTradeDetailsByClientReferenceForUser(clientReferenceNumber, principal.getName());
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
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.status(500).body("Error saving data: " + e.getMessage());
        }
    }

    @GetMapping("/exceptions/{clientReferenceNumber}")
    public List<TradeExceptionData> getExceptionsByClientReference(@PathVariable String clientReferenceNumber, Principal principal) {
        return storageService.getTradeExceptionsByClientReferenceForUser(clientReferenceNumber, principal.getName());
    }
}
