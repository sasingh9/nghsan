package com.example.oraclejson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

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
        Map<String, Object> result = jdbcTemplate.queryForMap("SELECT data, message_key FROM json_docs WHERE id = ?", 1L);

        byte[] savedBytes = (byte[]) result.get("data");
        String savedJson = new String(savedBytes, StandardCharsets.UTF_8);
        assertThat(savedJson).isEqualTo(jsonMessage);

        String messageKey = (String) result.get("message_key");
        assertThat(messageKey).isNotNull();
        // Validate that it is a valid UUID
        assertThat(UUID.fromString(messageKey)).isNotNull();
    }
}
