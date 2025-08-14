package com.example.oraclejson;

import com.example.oraclejson.dto.ErrorType;
import com.example.oraclejson.dto.JsonData;
import com.example.oraclejson.dto.TradeDetails;
import com.example.oraclejson.dto.TradeExceptionData;
import com.example.oraclejson.repository.AppUserRepository;
import com.example.oraclejson.repository.UserFundEntitlementRepository;
import com.example.oraclejson.service.UniqueIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
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
    private final AppUserRepository appUserRepository;
    private final UserFundEntitlementRepository userFundEntitlementRepository;
    private final UniqueIdGenerator uniqueIdGenerator;

    @Value("${app.db.ddl.create-table}")
    private String createTableDdl;

    @Value("${app.db.ddl.create-trade-details-table}")
    private String createTradeDetailsTableDdl;

    @Value("${app.db.ddl.create-trade-exceptions-table}")
    private String createTradeExceptionsTableDdl;

    @Value("${app.db.ddl.create-user-table}")
    private String createUserTableDdl;

    @Value("${app.db.ddl.create-entitlements-table}")
    private String createEntitlementsTableDdl;

    @Value("${app.kafka.topic.json-output}")
    private String outputTopic;

    public DatabaseStorageService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate, AppUserRepository appUserRepository, UserFundEntitlementRepository userFundEntitlementRepository, UniqueIdGenerator uniqueIdGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.appUserRepository = appUserRepository;
        this.userFundEntitlementRepository = userFundEntitlementRepository;
        this.uniqueIdGenerator = uniqueIdGenerator;
    }

    @PostConstruct
    @Profile("!prod")
    public void init() {
        log.warn("<<<<<< EXECUTING DESTRUCTIVE DATABASE SETUP. THIS SHOULD NOT RUN IN A PRODUCTION ENVIRONMENT. >>>>>>");
        setupTable();
    }

    private void setupTable() {
        log.info("Setting up tables for JSON storage...");

        // Drop tables in reverse order of creation to respect foreign key constraints
        try {
            log.info("Dropping existing table 'user_fund_entitlements' (if it exists).");
            jdbcTemplate.execute("DROP TABLE user_fund_entitlements");
        } catch (Exception e) {
            log.info("Table 'user_fund_entitlements' did not exist, which is fine.");
        }
        try {
            log.info("Dropping existing table 'app_user' (if it exists).");
            jdbcTemplate.execute("DROP TABLE app_user");
        } catch (Exception e) {
            log.info("Table 'app_user' did not exist, which is fine.");
        }
        try {
            log.info("Dropping existing table 'trade_details' (if it exists).");
            jdbcTemplate.execute("DROP TABLE trade_details");
        } catch (Exception e) {
            log.info("Table 'trade_details' did not exist, which is fine.");
        }
        try {
            log.info("Dropping existing table 'trade_exceptions' (if it exists).");
            jdbcTemplate.execute("DROP TABLE trade_exceptions");
        } catch (Exception e) {
            log.info("Table 'trade_exceptions' did not exist, which is fine.");
        }
        try {
            log.info("Dropping existing table 'json_docs' (if it exists).");
            jdbcTemplate.execute("DROP TABLE json_docs");
        } catch (Exception e) {
            log.info("Table 'json_docs' did not exist, which is fine.");
        }

        // Create tables
        log.info("Creating new table 'json_docs'.");
        jdbcTemplate.execute(createTableDdl);
        log.info("Table 'json_docs' created successfully.");

        log.info("Creating new table 'trade_details'.");
        jdbcTemplate.execute(createTradeDetailsTableDdl);
        log.info("Table 'trade_details' created successfully.");

        log.info("Creating new table 'trade_exceptions'.");
        jdbcTemplate.execute(createTradeExceptionsTableDdl);
        log.info("Table 'trade_exceptions' created successfully.");

        log.info("Creating new table 'app_user'.");
        jdbcTemplate.execute(createUserTableDdl);
        log.info("Table 'app_user' created successfully.");

        log.info("Creating new table 'user_fund_entitlements'.");
        jdbcTemplate.execute(createEntitlementsTableDdl);
        log.info("Table 'user_fund_entitlements' created successfully.");
    }

    public String save(String jsonMessage) {
        log.info("Saving JSON message to the database...");

        if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
            log.warn("Received an empty or null message, not saving.");
            return null;
        }

        // Save raw JSON to json_docs table
        String sql = "INSERT INTO json_docs (message_key, data) VALUES (?, ?)";
        String messageKey = uniqueIdGenerator.generateUniqueId();
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

            if (isDuplicate(tradeDetails.getClientReferenceNumber())) {
                log.warn("Duplicate trade detected with client reference number: {}. Skipping processing.", tradeDetails.getClientReferenceNumber());
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
            saveTechnicalException(jsonMessage, e);
        }
        return messageKey;
    }

    private boolean isDuplicate(String clientReferenceNumber) {
        if (clientReferenceNumber == null || clientReferenceNumber.trim().isEmpty()) {
            // Cannot be a duplicate if it has no reference number.
            return false;
        }

        String sqlTradeDetails = "SELECT COUNT(*) FROM trade_details WHERE client_reference_number = ?";
        Integer tradeCount = jdbcTemplate.queryForObject(sqlTradeDetails, new Object[]{clientReferenceNumber}, Integer.class);

        String sqlTradeExceptions = "SELECT COUNT(*) FROM trade_exceptions WHERE client_reference_number = ?";
        Integer exceptionCount = jdbcTemplate.queryForObject(sqlTradeExceptions, new Object[]{clientReferenceNumber}, Integer.class);

        return (tradeCount != null && tradeCount > 0) || (exceptionCount != null && exceptionCount > 0);
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
        try {
            String failureReason = String.join(", ", errors);
            log.warn("Saving business exception for client reference {}. Reason: {}", tradeDetails.getClientReferenceNumber(), failureReason);

            String sql = "INSERT INTO trade_exceptions (client_reference_number, error_type, failed_trade_json, failure_reason) VALUES (?, ?, ?, ?)";
            byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
            SqlParameterValue dataParam = new SqlParameterValue(Types.BLOB, jsonBytes);

            jdbcTemplate.update(sql, tradeDetails.getClientReferenceNumber(), ErrorType.BUSINESS.name(), dataParam, failureReason);
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

            String sql = "INSERT INTO trade_exceptions (client_reference_number, error_type, failed_trade_json, failure_reason) VALUES (?, ?, ?, ?)";
            byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
            SqlParameterValue dataParam = new SqlParameterValue(Types.BLOB, jsonBytes);

            // For technical errors, client_reference_number may not be available. Storing as null.
            jdbcTemplate.update(sql, null, ErrorType.TECHNICAL.name(), dataParam, failureReason);
        } catch (Exception dbException) {
            log.error("CRITICAL: Failed to save technical exception to the database. The original error was: {}. Original message: {}",
                    originalException.getMessage(), jsonMessage, dbException);
        }
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

    public List<TradeExceptionData> getTradeExceptionsByClientReference(String clientReferenceNumber) {
        String sql = "SELECT * FROM trade_exceptions WHERE client_reference_number = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            long id = rs.getLong("id");
            String ref = rs.getString("client_reference_number");
            byte[] jsonBytes = rs.getBytes("failed_trade_json");
            String failedJson = new String(jsonBytes, StandardCharsets.UTF_8);
            String reason = rs.getString("failure_reason");
            ErrorType errorType = ErrorType.valueOf(rs.getString("error_type"));
            LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            return new TradeExceptionData(id, ref, failedJson, reason, errorType, createdAt);
        }, clientReferenceNumber);
    }

    // New methods for data entitlements

    private List<String> getEntitledFundNumbers(String username) {
        return appUserRepository.findByUsername(username)
                .map(user -> userFundEntitlementRepository.findByUser(user).stream()
                        .map(UserFundEntitlement::getFundNumber)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<JsonData> getDataByDateRangeForUser(LocalDateTime startDate, LocalDateTime endDate, String username) {
        // This method is a bit tricky since json_docs is not directly tied to a fund.
        // For now, we will return all data, assuming this endpoint is for admins or for raw data inspection.
        // A more sophisticated implementation might involve parsing the JSON on the fly, which would be slow.
        log.warn("getDataByDateRangeForUser is not filtering by fund entitlement as json_docs has no fund_number. Returning all data.");
        return getDataByDateRange(startDate, endDate);
    }

    public List<TradeDetails> getTradeDetailsByClientReferenceForUser(String clientReferenceNumber, String username) {
        List<String> entitledFunds = getEntitledFundNumbers(username);
        if (entitledFunds.isEmpty()) {
            return Collections.emptyList();
        }

        String inSql = String.join(",", Collections.nCopies(entitledFunds.size(), "?"));
        String sql = String.format("SELECT * FROM trade_details WHERE client_reference_number = ? AND fund_number IN (%s)", inSql);

        List<Object> params = new ArrayList<>();
        params.add(clientReferenceNumber);
        params.addAll(entitledFunds);

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> {
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
        });
    }

    public List<TradeExceptionData> getTradeExceptionsByClientReferenceForUser(String clientReferenceNumber, String username) {
        // Note: Filtering exceptions can be tricky if the fund_number is not stored on the exception record.
        // We are assuming that we can join with trade_details or parse the JSON to get the fund.
        // For now, let's assume exceptions are not filtered by fund, as they represent processing errors
        // that an operations user might need to see regardless of fund.
        // This is a design decision that might need to be revisited.
        log.warn("getTradeExceptionsByClientReferenceForUser is not filtering by fund entitlement. This is a design decision.");
        return getTradeExceptionsByClientReference(clientReferenceNumber);
    }
}
