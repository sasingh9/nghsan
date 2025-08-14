package com.example.oraclejson;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JsonStorageRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(JsonStorageRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public JsonStorageRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting JSON storage demonstration.");

        setupTable();
        insertJsonDocument();
        retrieveAndPrintJson();

        log.info("JSON storage demonstration finished.");
    }

    private void setupTable() {
        log.info("Setting up table 'json_docs'...");
        try {
            log.info("Dropping existing table 'json_docs' (if it exists).");
            jdbcTemplate.execute("DROP TABLE json_docs");
        } catch (Exception e) {
            log.info("Table 'json_docs' did not exist, which is fine.");
            // Ignore exception if table doesn't exist
        }

        log.info("Creating new table 'json_docs' with BLOB storage for JSON.");
        jdbcTemplate.execute("CREATE TABLE json_docs (id NUMBER PRIMARY KEY, data BLOB CHECK (data IS JSON))");
        log.info("Table 'json_docs' created successfully.");
    }

    private void insertJsonDocument() {
        log.info("Creating and inserting a sample JSON document...");

        // Create a JSON object using Oracle's JSON library
        OracleJsonFactory factory = new OracleJsonFactory();
        OracleJsonObject jsonObject = factory.createObject();
        jsonObject.put("name", "John Doe");
        jsonObject.put("email", "john.doe@example.com");
        jsonObject.put("age", 30);

        String sql = "INSERT INTO json_docs (id, data) VALUES (?, ?)";

        // Convert JSON object to string and then to UTF-8 bytes for BLOB storage
        byte[] jsonBytes = jsonObject.toString().getBytes(StandardCharsets.UTF_8);

        jdbcTemplate.update(sql, 1, jsonBytes);

        log.info("Successfully inserted JSON document.");
    }

    private void retrieveAndPrintJson() {
        log.info("Retrieving JSON document from the database...");
        String sql = "SELECT data FROM json_docs WHERE id = ?";

        byte[] retrievedBytes = jdbcTemplate.queryForObject(sql, byte[].class, 1);

        if (retrievedBytes != null) {
            String retrievedJson = new String(retrievedBytes, StandardCharsets.UTF_8);
            log.info("Retrieved JSON document:\n" + retrievedJson);
        } else {
            log.warn("No JSON document found for id = 1.");
        }
    }
}
