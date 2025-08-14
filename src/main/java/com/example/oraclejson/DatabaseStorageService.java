package com.example.oraclejson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import org.springframework.jdbc.core.SqlParameterValue;

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

        // A simple validation to ensure the string is not empty
        if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
            log.warn("Received an empty or null message, not saving.");
            return;
        }

        String sql = "INSERT INTO json_docs (data) VALUES (?)";

        byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);

        // Use SqlParameterValue to specify the SQL type for the BLOB
        SqlParameterValue param = new SqlParameterValue(Types.BLOB, jsonBytes);
        jdbcTemplate.update(sql, param);

        log.info("Successfully saved JSON message to the database.");
    }
}
