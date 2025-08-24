package com.poc.trademanager.service;

import com.poc.trademanager.dto.ErrorType;
import com.poc.trademanager.dto.TradeDetailsDto;
import com.poc.trademanager.entity.Fund;
import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.entity.TradeDetail;
import com.poc.trademanager.entity.TradeException;
import com.poc.trademanager.repository.FundRepository;
import com.poc.trademanager.repository.TradeDetailRepository;
import com.poc.trademanager.repository.TradeExceptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MessageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessingService.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TradeDetailRepository tradeDetailRepository;
    private final TradeExceptionRepository tradeExceptionRepository;
    private final FundRepository fundRepository;

    @Value("${app.kafka.topic.json-output}")
    private String outputTopic;

    public MessageProcessingService(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate, TradeDetailRepository tradeDetailRepository, TradeExceptionRepository tradeExceptionRepository, FundRepository fundRepository) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.tradeDetailRepository = tradeDetailRepository;
        this.tradeExceptionRepository = tradeExceptionRepository;
        this.fundRepository = fundRepository;
    }

    @Async("asyncTaskExecutor")
    @Transactional
    public void processMessage(JsonDoc jsonDoc) {
        long startTime = System.currentTimeMillis();
        log.info("Starting async processing for message key: {}", jsonDoc.getMessageKey());
        String jsonMessage = jsonDoc.getData();

        try {
            TradeDetailsDto tradeDetailsDto = objectMapper.readValue(jsonMessage, TradeDetailsDto.class);

            if (tradeDetailsDto.getClientReferenceNumber() == null) {
                log.warn("Trade details has no client reference number, skipping validation and saving.");
                return;
            }

            if (isDuplicate(tradeDetailsDto.getClientReferenceNumber())) {
                log.warn("Duplicate trade detected with client reference number: {}. Skipping processing.", tradeDetailsDto.getClientReferenceNumber());
                return;
            }

            // Enrich with fund base currency
            Optional<Fund> fundOptional = fundRepository.findById(tradeDetailsDto.getFundNumber());
            if (fundOptional.isPresent()) {
                tradeDetailsDto.setBaseCurrency(fundOptional.get().getBaseCurrency());
            } else {
                saveException(tradeDetailsDto, jsonMessage, List.of("Fund not found"));
                return;
            }

            List<String> validationErrors = validateTradeDetails(tradeDetailsDto);

            if (validationErrors.isEmpty()) {
                TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setClientReferenceNumber(tradeDetailsDto.getClientReferenceNumber());
                tradeDetail.setFundNumber(tradeDetailsDto.getFundNumber());
                tradeDetail.setSecurityId(tradeDetailsDto.getSecurityId());
                tradeDetail.setTradeDate(tradeDetailsDto.getTradeDate());
                tradeDetail.setSettleDate(tradeDetailsDto.getSettleDate());
                tradeDetail.setQuantity(tradeDetailsDto.getQuantity());
                tradeDetail.setPrice(tradeDetailsDto.getPrice());
                tradeDetail.setPrincipal(tradeDetailsDto.getPrincipal());
                tradeDetail.setNetAmount(tradeDetailsDto.getNetAmount());
                String tradeDetailsJson = objectMapper.writeValueAsString(tradeDetailsDto);
                tradeDetail.setOutboundJson(tradeDetailsJson);
                tradeDetailRepository.save(tradeDetail);
                log.info("Successfully extracted and saved trade details for client reference: {}", tradeDetail.getClientReferenceNumber());

                kafkaTemplate.send(outputTopic, tradeDetailsJson);
                log.info("Successfully published trade details to Kafka topic {}: {}", outputTopic, tradeDetailsJson);
            } else {
                saveException(tradeDetailsDto, jsonMessage, validationErrors);
            }
        } catch (JsonProcessingException e) {
            saveTechnicalException(jsonMessage, e);
        } catch (Exception e) {
            saveTechnicalException(jsonMessage, e);
        }

        long endTime = System.currentTimeMillis();
        log.info("Finished async processing for message key: {}. Time taken: {} ms", jsonDoc.getMessageKey(), (endTime - startTime));
    }

    private boolean isDuplicate(String clientReferenceNumber) {
        if (clientReferenceNumber == null || clientReferenceNumber.trim().isEmpty()) {
            return false;
        }
        return tradeDetailRepository.existsByClientReferenceNumber(clientReferenceNumber) ||
               tradeExceptionRepository.existsByClientReferenceNumber(clientReferenceNumber);
    }

    private List<String> validateTradeDetails(TradeDetailsDto tradeDetails) {
        List<String> errors = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (tradeDetails.getTradeDate() != null) {
            LocalDate tradeDate = tradeDetails.getTradeDate();
            if (!tradeDate.equals(today)) {
                errors.add("Trade date (" + tradeDate + ") is not the current date (" + today + ").");
            }
        } else {
            errors.add("Trade date is null.");
        }

        if (tradeDetails.getSettleDate() != null) {
            LocalDate settleDate = tradeDetails.getSettleDate();
            if (!settleDate.isAfter(today)) {
                errors.add("Settlement date (" + settleDate + ") is not in the future.");
            }
        } else {
            errors.add("Settlement date is null.");
        }

        if (tradeDetails.getQuantity() != null && tradeDetails.getPrice() != null && tradeDetails.getPrincipal() != null) {
            BigDecimal calculatedPrincipal = tradeDetails.getQuantity().multiply(tradeDetails.getPrice()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal providedPrincipal = tradeDetails.getPrincipal().setScale(4, RoundingMode.HALF_UP);

            if (calculatedPrincipal.compareTo(providedPrincipal) != 0) {
                errors.add("Principal amount (" + providedPrincipal + ") does not equal quantity * price (" + calculatedPrincipal + ").");
            }
        } else {
            errors.add("Quantity, price, or principal is null.");
        }

        return errors;
    }

    private void saveException(TradeDetailsDto tradeDetails, String jsonMessage, List<String> errors) {
        try {
            String failureReason = String.join(", ", errors);
            log.warn("Saving business exception for client reference {}. Reason: {}", tradeDetails.getClientReferenceNumber(), failureReason);

            TradeException tradeException = new TradeException();
            tradeException.setClientReferenceNumber(tradeDetails.getClientReferenceNumber());
            tradeException.setErrorType(ErrorType.BUSINESS);
            tradeException.setFailedTradeJson(jsonMessage);
            tradeException.setFailureReason(failureReason);
            tradeExceptionRepository.save(tradeException);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to save business exception to the database. Client Reference: {}. Reason: {}. Original message: {}",
                    tradeDetails.getClientReferenceNumber(), errors, jsonMessage, e);
        }
    }

    private void saveTechnicalException(String jsonMessage, Exception originalException) {
        try {
            log.error("Saving technical exception for trade message: {}", jsonMessage, originalException);
            String failureReason = originalException.getClass().getSimpleName() + ": " + originalException.getMessage();
            if (failureReason.length() > 1000) {
                failureReason = failureReason.substring(0, 997) + "...";
            }

            TradeException tradeException = new TradeException();
            tradeException.setErrorType(ErrorType.TECHNICAL);
            tradeException.setFailedTradeJson(jsonMessage);
            tradeException.setFailureReason(failureReason);
            tradeExceptionRepository.save(tradeException);
        } catch (Exception dbException) {
            log.error("CRITICAL: Failed to save technical exception to the database. The original error was: {}. Original message: {}",
                    originalException.getMessage(), jsonMessage, dbException);
        }
    }
}
