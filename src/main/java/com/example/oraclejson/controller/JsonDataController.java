package com.example.oraclejson.controller;

import com.example.oraclejson.DatabaseStorageService;
import com.example.oraclejson.dto.ApiResponse;
import com.example.oraclejson.dto.ErrorResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class JsonDataController {

    private final DatabaseStorageService storageService;

    public JsonDataController(DatabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Page<JsonData>>> getData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date.");
            }
            long daysBetween = java.time.Duration.between(startDate, endDate).toDays();
            if (daysBetween > 31) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The date range cannot exceed 31 days.");
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Pageable pageable = PageRequest.of(page, size);
        Page<JsonData> data = storageService.getDataByDateRangeForUser(startDate, endDate, username, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Data retrieved successfully", data));
    }

    @GetMapping("/trades/{clientReferenceNumber}")
    public ResponseEntity<ApiResponse<List<TradeDetails>>> getTradesByClientReference(@PathVariable String clientReferenceNumber) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<TradeDetails> trades = storageService.getTradeDetailsByClientReferenceForUser(clientReferenceNumber, username);
        return ResponseEntity.ok(new ApiResponse<>(true, "Trades retrieved successfully", trades));
    }

    /**
     * A temporary endpoint for manually posting data for testing purposes.
     * In a real application, this data would come from a Kafka topic.
     */
    @PostMapping("/data")
    public ResponseEntity<?> receiveData(@RequestBody String jsonData) {
        try {
            String messageKey = storageService.save(jsonData);
            if (messageKey != null) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Data saved successfully", messageKey));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("SAVE_ERROR", "Could not save empty or null data.", "Ensure the request body is not empty."));
            }
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("JSON_PARSE_ERROR", "Invalid JSON format: " + e.getMessage(), "Check syntax and try again"));
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.status(500).body(new ErrorResponse("INTERNAL_SERVER_ERROR", "Error saving data: " + e.getMessage(), "Contact support."));
        }
    }

    @GetMapping("/exceptions/{clientReferenceNumber}")
    public ResponseEntity<ApiResponse<List<TradeExceptionData>>> getExceptionsByClientReference(@PathVariable String clientReferenceNumber) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<TradeExceptionData> exceptions = storageService.getTradeExceptionsByClientReferenceForUser(clientReferenceNumber, username);
        return ResponseEntity.ok(new ApiResponse<>(true, "Exceptions retrieved successfully", exceptions));
    }
}
