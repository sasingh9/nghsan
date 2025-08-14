package com.example.oraclejson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import com.example.oraclejson.dto.JsonData;
import org.springframework.jdbc.core.SqlParameterValue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DatabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStorageService.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.db.ddl.create-table}")
    private String createTableDdl;

    public DatabaseStorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        setupTable();
    }

    private void setupTable() {
        log.info("Setting up table 'json_docs' for JSON storage...");
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
    }

    public void save(String jsonMessage) {
        log.info("Saving JSON message to the database...");

        if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
            log.warn("Received an empty or null message, not saving.");
            return;
        }

        String sql = "INSERT INTO json_docs (message_key, data) VALUES (?, ?)";

        String messageKey = UUID.randomUUID().toString();
        byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);

        SqlParameterValue dataParam = new SqlParameterValue(Types.BLOB, jsonBytes);

        jdbcTemplate.update(sql, messageKey, dataParam);

        log.info("Successfully saved JSON message with key {} to the database.", messageKey);
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
