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
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.LocalDateTime;
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
    }

    public void save(String jsonMessage) {
        log.info("Saving JSON message to the database...");

        if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
            log.warn("Received an empty or null message, not saving.");
            return;
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
            if (tradeDetails.getClientReferenceNumber() != null) {
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
            }
        } catch (Exception e) {
            log.error("Failed to parse and save trade details from JSON message: {}", jsonMessage, e);
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
}
