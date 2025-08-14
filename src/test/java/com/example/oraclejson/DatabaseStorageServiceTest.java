package com.example.oraclejson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseStorageServiceTest {

    @Autowired
    private DatabaseStorageService databaseStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testSaveJsonMessage() {
        // Given
        String jsonMessage = "{\"key\":\"value\"}";

        // When
        databaseStorageService.save(jsonMessage);

        // Then
        byte[] savedBytes = jdbcTemplate.queryForObject("SELECT data FROM json_docs WHERE id = ?", byte[].class, 1L);
        String savedJson = new String(savedBytes, StandardCharsets.UTF_8);

        assertThat(savedJson).isEqualTo(jsonMessage);
    }
}
