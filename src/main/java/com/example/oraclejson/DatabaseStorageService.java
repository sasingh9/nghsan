package com.example.oraclejson;

import com.example.oraclejson.dto.JsonData;
import com.example.oraclejson.dto.TradeDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DatabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStorageService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.db.ddl.create-table}")
    private String createTableDdl;

    @Value("${app.db.ddl.create-trade-details-table}")
    private String createTradeDetailsTableDdl;

    @Value("${app.db.ddl.create-trade-exceptions-table}")
    private String createTradeExceptionsTableDdl;

    @Value("${app.kafka.topic.json-output}")
    private String outputTopic;

    public DatabaseStorageService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void init() {
        setupTable();
    }

    private void setupTable() {
        log.info("Setting up tables for JSON storage...");
        try {
            log.info("Dropping existing table 'trade_details' (if it exists).");
            jdbcTemplate.execute("DROP TABLE trade_details");
        } catch (Exception e) {
            log.info("Table 'trade_details' did not exist, which is fine.");
        }

        try {
            log.info("Dropping existing table 'json_docs' (if it exists).");
            jdbcTemplate.execute("DROP TABLE json_docs");
        } catch (Exception e) {
            log.info("Table 'json_docs' did not exist, which is fine.");
            // Ignore exception if table doesn't exist
        }

        log.info("Creating new table 'json_docs'.");
        jdbcTemplate.execute(createTableDdl);
        log.info("Table 'json_docs' created successfully.");

        log.info("Creating new table 'trade_details'.");
        jdbcTemplate.execute(createTradeDetailsTableDdl);
        log.info("Table 'trade_details' created successfully.");

        try {
            log.info("Dropping existing table 'trade_exceptions' (if it exists).");
            jdbcTemplate.execute("DROP TABLE trade_exceptions");
        } catch (Exception e) {
            log.info("Table 'trade_exceptions' did not exist, which is fine.");
        }

        log.info("Creating new table 'trade_exceptions'.");
        jdbcTemplate.execute(createTradeExceptionsTableDdl);
        log.info("Table 'trade_exceptions' created successfully.");
    }

    public String save(String jsonMessage) {
        log.info("Saving JSON message to the database...");

        if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
            log.warn("Received an empty or null message, not saving.");
            return null;
        }

        // Save raw JSON to json_docs table
        String sql = "INSERT INTO json_docs (message_key, data) VALUES (?, ?)";
        String messageKey = UUID.randomUUID().toString();
        byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
        SqlParameterValue dataParam = new SqlParameterValue(Types.BLOB, jsonBytes);
        jdbcTemplate.update(sql, messageKey, dataParam);
        log.info("Successfully saved JSON message with key {} to the database.", messageKey);

        // Parse and save to trade_details table
        try {
            TradeDetails tradeDetails = objectMapper.readValue(jsonMessage, TradeDetails.class);

            if (tradeDetails.getClientReferenceNumber() == null) {
                log.warn("Trade details has no client reference number, skipping validation and saving.");
                return messageKey;
            }

            List<String> validationErrors = validateTradeDetails(tradeDetails);

            if (validationErrors.isEmpty()) {
                // Save to trade_details table
                String sqlTradeDetails = "INSERT INTO trade_details (client_reference_number, fund_number, security_id, trade_date, settle_date, quantity, price, principal, net_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(sqlTradeDetails,
                        tradeDetails.getClientReferenceNumber(),
                        tradeDetails.getFundNumber(),
                        tradeDetails.getSecurityId(),
                        tradeDetails.getTradeDate(),
                        tradeDetails.getSettleDate(),
                        tradeDetails.getQuantity(),
                        tradeDetails.getPrice(),
                        tradeDetails.getPrincipal(),
                        tradeDetails.getNetAmount());
                log.info("Successfully extracted and saved trade details for client reference: {}", tradeDetails.getClientReferenceNumber());

                // Publish trade details to Kafka
                String tradeDetailsJson = objectMapper.writeValueAsString(tradeDetails);
                kafkaTemplate.send(outputTopic, tradeDetailsJson);
                log.info("Successfully published trade details to Kafka topic {}: {}", outputTopic, tradeDetailsJson);
            } else {
                saveException(tradeDetails, jsonMessage, validationErrors);
            }
        } catch (Exception e) {
            log.error("Failed to process trade message: {}", jsonMessage, e);
        }
        return messageKey;
    }

    private List<String> validateTradeDetails(TradeDetails tradeDetails) {
        List<String> errors = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Rule 1: Trade date must be the current date.
        if (tradeDetails.getTradeDate() != null) {
            LocalDate tradeDate = tradeDetails.getTradeDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            if (!tradeDate.equals(today)) {
                errors.add("Trade date (" + tradeDate + ") is not the current date (" + today + ").");
            }
        } else {
            errors.add("Trade date is null.");
        }

        // Rule 2: Settlement date must be in the future.
        if (tradeDetails.getSettleDate() != null) {
            LocalDate settleDate = tradeDetails.getSettleDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            if (!settleDate.isAfter(today)) {
                errors.add("Settlement date (" + settleDate + ") is not in the future.");
            }
        } else {
            errors.add("Settlement date is null.");
        }

        // Rule 3: Principal should be equal to quantity multiplied by price.
        if (tradeDetails.getQuantity() != null && tradeDetails.getPrice() != null && tradeDetails.getPrincipal() != null) {
            // Use a consistent scale for comparison, based on the database schema.
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

    private void saveException(TradeDetails tradeDetails, String jsonMessage, List<String> errors) {
        String failureReason = String.join(", ", errors);
        log.warn("Saving trade exception for client reference {}. Reason: {}", tradeDetails.getClientReferenceNumber(), failureReason);

        String sql = "INSERT INTO trade_exceptions (client_reference_number, failed_trade_json, failure_reason) VALUES (?, ?, ?)";
        byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
        SqlParameterValue dataParam = new SqlParameterValue(Types.BLOB, jsonBytes);

        jdbcTemplate.update(sql, tradeDetails.getClientReferenceNumber(), dataParam, failureReason);
    }

    public List<JsonData> getDataByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT id, message_key, data, created_at FROM json_docs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (startDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(startDate);
        }

        if (endDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(endDate);
        }

        sql.append(" ORDER BY created_at DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            long id = rs.getLong("id");
            String messageKey = rs.getString("message_key");
            byte[] dataBytes = rs.getBytes("data");
            String jsonData = new String(dataBytes, StandardCharsets.UTF_8);
            LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            return new JsonData(id, messageKey, jsonData, createdAt);
        });
    }

    public List<TradeDetails> getTradeDetailsByClientReference(String clientReferenceNumber) {
        String sql = "SELECT * FROM trade_details WHERE client_reference_number = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TradeDetails trade = new TradeDetails();
            trade.setClientReferenceNumber(rs.getString("client_reference_number"));
            trade.setFundNumber(rs.getString("fund_number"));
            trade.setSecurityId(rs.getString("security_id"));
            trade.setTradeDate(rs.getDate("trade_date"));
            trade.setSettleDate(rs.getDate("settle_date"));
            trade.setQuantity(rs.getBigDecimal("quantity"));
            trade.setPrice(rs.getBigDecimal("price"));
            trade.setPrincipal(rs.getBigDecimal("principal"));
            trade.setNetAmount(rs.getBigDecimal("net_amount"));
            return trade;
        }, clientReferenceNumber);
    }
}
