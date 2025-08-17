package com.poc.trademanager;

import com.poc.trademanager.dto.JsonData;
import com.poc.trademanager.dto.TradeDetailsDto;
import com.poc.trademanager.dto.TradeExceptionData;
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
import java.util.Collections;
import java.util.List;
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

    public DatabaseStorageService(AppUserRepository appUserRepository, UserFundEntitlementRepository userFundEntitlementRepository, UniqueIdGenerator uniqueIdGenerator, JsonDocRepository jsonDocRepository, TradeDetailRepository tradeDetailRepository, TradeExceptionRepository tradeExceptionRepository) {
        this.appUserRepository = appUserRepository;
        this.userFundEntitlementRepository = userFundEntitlementRepository;
        this.uniqueIdGenerator = uniqueIdGenerator;
        this.jsonDocRepository = jsonDocRepository;
        this.tradeDetailRepository = tradeDetailRepository;
        this.tradeExceptionRepository = tradeExceptionRepository;
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

    public List<TradeDetailsDto> getTradeDetailsByClientReferenceForUser(String clientReferenceNumber, String username) {
        List<String> entitledFunds = getEntitledFundNumbers(username);
        if (entitledFunds.isEmpty()) {
            return Collections.emptyList();
        }
        return tradeDetailRepository.findByClientReferenceNumberAndFundNumberIn(clientReferenceNumber, entitledFunds).stream()
                .map(this::convertToTradeDetailsDto)
                .collect(Collectors.toList());
    }

    public List<TradeExceptionData> getTradeExceptionsByClientReferenceForUser(String clientReferenceNumber, String username) {
        log.warn("getTradeExceptionsByClientReferenceForUser is not filtering by fund entitlement. This is a design decision.");
        return getTradeExceptionsByClientReference(clientReferenceNumber);
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
}
