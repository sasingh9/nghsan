package com.poc.trademanager.controller;

import com.poc.trademanager.DatabaseStorageService;
import com.poc.trademanager.dto.ApiResponse;
import com.poc.trademanager.dto.ErrorResponse;
import com.poc.trademanager.dto.JsonData;
import com.poc.trademanager.dto.TradeDetailsDto;
import com.poc.trademanager.dto.TradeExceptionData;
import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.service.MessageProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class JsonDataController {

    private static final Logger log = LoggerFactory.getLogger(JsonDataController.class);
    private final DatabaseStorageService storageService;
    private final MessageProcessingService messageProcessingService;

    public JsonDataController(DatabaseStorageService storageService, MessageProcessingService messageProcessingService) {
        this.storageService = storageService;
        this.messageProcessingService = messageProcessingService;
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Page<JsonData>>> getData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date.");
            }
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 31) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The date range cannot exceed 31 days.");
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<JsonData> data = storageService.getDataByDateRangeForUser(startDate, endDate, username, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Data retrieved successfully", data));
    }

    @GetMapping("/trades/{clientReferenceNumber}")
    public ResponseEntity<ApiResponse<List<TradeDetailsDto>>> getTradesByClientReference(@PathVariable String clientReferenceNumber) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<TradeDetailsDto> trades = storageService.getTradeDetailsByClientReferenceForUser(clientReferenceNumber, username);
        return ResponseEntity.ok(new ApiResponse<>(true, "Trades retrieved successfully", trades));
    }

    /**
     * A temporary endpoint for manually posting data for testing purposes.
     * In a real application, this data would come from a Kafka topic.
     */
    @PostMapping("/data")
    @PreAuthorize("hasRole('SUPPORT') or hasAuthority('MANUAL_PROCESSING')")
    public ResponseEntity<?> receiveData(@RequestBody String jsonData) {
        try {
            // This endpoint is for manual submission, so we call the processing service directly.
            // In a real-world scenario, this might publish to Kafka instead.
            // For now, we will save the raw message and then process it.
            JsonDoc jsonDoc = storageService.saveRawMessage(jsonData);
            if (jsonDoc != null) {
                messageProcessingService.processMessage(jsonDoc);
                return ResponseEntity.ok(new ApiResponse<>(true, "Data accepted for processing.", jsonDoc.getMessageKey()));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("SAVE_ERROR", "Could not save empty or null data.", "Ensure the request body is not empty."));
            }
        } catch (Exception e) {
            log.error("Error in manual data submission", e);
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
