package com.poc.trademanager;

import com.poc.trademanager.dto.FundTradeCount;
import com.poc.trademanager.dto.JsonData;
import com.poc.trademanager.dto.TradeDetailsDto;
import com.poc.trademanager.dto.TradeExceptionData;
import com.poc.trademanager.dto.TradeSummaryDto;
import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.entity.TradeDetail;
import com.poc.trademanager.entity.TradeException;
import com.poc.trademanager.entity.UserFundEntitlement;
import com.poc.trademanager.repository.AppUserRepository;
import com.poc.trademanager.repository.JsonDocRepository;
import com.poc.trademanager.repository.TradeDetailRepository;
import com.poc.trademanager.repository.TradeExceptionRepository;
import com.poc.trademanager.repository.UserFundEntitlementRepository;
import com.poc.trademanager.service.UniqueIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DatabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStorageService.class);

    private final AppUserRepository appUserRepository;
    private final UserFundEntitlementRepository userFundEntitlementRepository;
    private final UniqueIdGenerator uniqueIdGenerator;
    private final JsonDocRepository jsonDocRepository;
    private final TradeDetailRepository tradeDetailRepository;
    private final TradeExceptionRepository tradeExceptionRepository;
    private final ObjectMapper objectMapper;

    public DatabaseStorageService(AppUserRepository appUserRepository, UserFundEntitlementRepository userFundEntitlementRepository, UniqueIdGenerator uniqueIdGenerator, JsonDocRepository jsonDocRepository, TradeDetailRepository tradeDetailRepository, TradeExceptionRepository tradeExceptionRepository, ObjectMapper objectMapper) {
        this.appUserRepository = appUserRepository;
        this.userFundEntitlementRepository = userFundEntitlementRepository;
        this.uniqueIdGenerator = uniqueIdGenerator;
        this.jsonDocRepository = jsonDocRepository;
        this.tradeDetailRepository = tradeDetailRepository;
        this.tradeExceptionRepository = tradeExceptionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Retryable(
        value = { DataAccessException.class },
        maxAttempts = Integer.MAX_VALUE,
        backoff = @Backoff(delay = 300000)
    )
    public JsonDoc saveRawMessage(String jsonMessage) {
        if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
            log.warn("Received an empty or null message, not saving.");
            return null;
        }
        JsonDoc jsonDoc = new JsonDoc();
        jsonDoc.setMessageKey(uniqueIdGenerator.generateUniqueId());
        jsonDoc.setData(jsonMessage);
        return jsonDocRepository.save(jsonDoc);
    }

    public Page<JsonData> getDataByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        if (startDateTime != null && endDateTime != null) {
            return jsonDocRepository.findByCreatedAtBetween(startDateTime, endDateTime, pageable).map(this::convertToJsonData);
        } else if (startDateTime != null) {
            return jsonDocRepository.findByCreatedAtGreaterThanEqual(startDateTime, pageable).map(this::convertToJsonData);
        } else if (endDateTime != null) {
            return jsonDocRepository.findByCreatedAtLessThanEqual(endDateTime, pageable).map(this::convertToJsonData);
        } else {
            return jsonDocRepository.findAll(pageable).map(this::convertToJsonData);
        }
    }

    public List<TradeDetailsDto> getTradeDetailsByClientReference(String clientReferenceNumber) {
        return tradeDetailRepository.findByClientReferenceNumber(clientReferenceNumber).stream()
                .map(this::convertToTradeDetailsDto)
                .collect(Collectors.toList());
    }

    public List<TradeExceptionData> getTradeExceptionsByClientReference(String clientReferenceNumber) {
        return tradeExceptionRepository.findByClientReferenceNumber(clientReferenceNumber).stream()
                .map(this::convertToTradeExceptionData)
                .collect(Collectors.toList());
    }

    private List<String> getEntitledFundNumbers(String username) {
        return appUserRepository.findByUsername(username)
                .map(user -> userFundEntitlementRepository.findByUser(user).stream()
                        .map(UserFundEntitlement::getFundNumber)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public Page<JsonData> getDataByDateRangeForUser(LocalDate startDate, LocalDate endDate, String username, Pageable pageable) {
        log.warn("getDataByDateRangeForUser is not filtering by fund entitlement as json_docs has no fund_number. Returning all data.");
        return getDataByDateRange(startDate, endDate, pageable);
    }

    public List<TradeDetailsDto> getTradeDetailsForUser(String clientReferenceNumber, String username, LocalDate startDate, LocalDate endDate) {
        List<String> entitledFunds = getEntitledFundNumbers(username);
        if (entitledFunds.isEmpty()) {
            return Collections.emptyList();
        }

        List<TradeDetail> trades;
        boolean hasClientRef = clientReferenceNumber != null && !clientReferenceNumber.trim().isEmpty();
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasClientRef && hasDateRange) {
            trades = tradeDetailRepository.findByClientReferenceNumberAndFundNumberInAndTradeDateBetween(clientReferenceNumber, entitledFunds, startDate, endDate);
        } else if (hasClientRef) {
            trades = tradeDetailRepository.findByClientReferenceNumberAndFundNumberIn(clientReferenceNumber, entitledFunds);
        } else if (hasDateRange) {
            trades = tradeDetailRepository.findByFundNumberInAndTradeDateBetween(entitledFunds, startDate, endDate);
        } else {
            // Should be prevented by controller, but as a safeguard:
            trades = Collections.emptyList();
        }

        return trades.stream()
                .map(this::convertToTradeDetailsDto)
                .collect(Collectors.toList());
    }

    public List<TradeExceptionData> getTradeExceptionsForUser(String clientReferenceNumber, String username, LocalDateTime startDate, LocalDateTime endDate) {
        log.warn("getTradeExceptionsForUser is not filtering by fund entitlement. This is a design decision.");

        List<TradeException> exceptions;
        boolean hasClientRef = clientReferenceNumber != null && !clientReferenceNumber.trim().isEmpty();
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasClientRef && hasDateRange) {
            exceptions = tradeExceptionRepository.findByClientReferenceNumberAndCreatedAtBetween(clientReferenceNumber, startDate, endDate);
        } else if (hasClientRef) {
            exceptions = tradeExceptionRepository.findByClientReferenceNumber(clientReferenceNumber);
        } else if (hasDateRange) {
            exceptions = tradeExceptionRepository.findByCreatedAtBetween(startDate, endDate);
        } else {
            // Should be prevented by controller, but as a safeguard:
            exceptions = Collections.emptyList();
        }

        return exceptions.stream()
                .map(this::convertToTradeExceptionData)
                .collect(Collectors.toList());
    }

    private JsonData convertToJsonData(JsonDoc jsonDoc) {
        return new JsonData(jsonDoc.getId(), jsonDoc.getMessageKey(), jsonDoc.getData(), jsonDoc.getCreatedAt());
    }

    private TradeDetailsDto convertToTradeDetailsDto(TradeDetail tradeDetail) {
        TradeDetailsDto dto = new TradeDetailsDto();
        dto.setClientReferenceNumber(tradeDetail.getClientReferenceNumber());
        dto.setFundNumber(tradeDetail.getFundNumber());
        dto.setSecurityId(tradeDetail.getSecurityId());
        dto.setTradeDate(tradeDetail.getTradeDate());
        dto.setSettleDate(tradeDetail.getSettleDate());
        dto.setQuantity(tradeDetail.getQuantity());
        dto.setPrice(tradeDetail.getPrice());
        dto.setPrincipal(tradeDetail.getPrincipal());
        dto.setNetAmount(tradeDetail.getNetAmount());
        return dto;
    }

    private TradeExceptionData convertToTradeExceptionData(TradeException tradeException) {
        return new TradeExceptionData(
                tradeException.getId(),
                tradeException.getClientReferenceNumber(),
                tradeException.getFailedTradeJson(),
                tradeException.getFailureReason(),
                tradeException.getErrorType(),
                tradeException.getCreatedAt()
        );
    }

    public List<TradeSummaryDto> getTradeSummary() {
        List<FundTradeCount> createdCounts = tradeDetailRepository.countByFundNumber();
        List<TradeException> exceptions = tradeExceptionRepository.findAll();

        Map<String, Long> exceptionCounts = exceptions.stream()
                .collect(Collectors.groupingBy(ex -> {
                    try {
                        Map<String, Object> tradeData = objectMapper.readValue(ex.getFailedTradeJson(), Map.class);
                        return (String) tradeData.getOrDefault("fundNumber", "UNKNOWN");
                    } catch (Exception e) {
                        log.error("Error parsing failed trade JSON for exception ID {}: {}", ex.getId(), e.getMessage());
                        return "UNKNOWN";
                    }
                }, Collectors.counting()));

        Map<String, TradeSummaryDto> summaryMap = createdCounts.stream()
                .collect(Collectors.toMap(
                        FundTradeCount::getFundNumber,
                        dto -> new TradeSummaryDto(dto.getFundNumber(), dto.getCount(), dto.getCount(), 0L)
                ));

        exceptionCounts.forEach((fundNumber, count) -> {
            summaryMap.computeIfAbsent(fundNumber, fn -> new TradeSummaryDto(fn, 0L, 0L, 0L));
            TradeSummaryDto summary = summaryMap.get(fundNumber);
            summary.setExceptions(count);
            summary.setTradesReceived(summary.getTradesCreated() + count);
        });

        return new ArrayList<>(summaryMap.values());
    }
}
