package com.example.oraclejson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
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

    @Test
    void testSaveTradeDetails() {
        // Given
        String jsonMessage = "{\"client_reference_number\":\"CLIENT-001\",\"fund_number\":\"FUND-A\",\"security_id\":\"SEC-12345\",\"trade_date\":\"2023-10-26T10:00:00Z\",\"settle_date\":\"2023-10-28T10:00:00Z\",\"quantity\":100.5,\"price\":12.34,\"principal\":1240.17,\"net_amount\":1250.00}";

        // When
        databaseStorageService.save(jsonMessage);

        // Then
        Map<String, Object> result = jdbcTemplate.queryForMap("SELECT * FROM trade_details WHERE client_reference_number = ?", "CLIENT-001");

        assertThat(result.get("CLIENT_REFERENCE_NUMBER")).isEqualTo("CLIENT-001");
        assertThat(result.get("FUND_NUMBER")).isEqualTo("FUND-A");
        assertThat(result.get("SECURITY_ID")).isEqualTo("SEC-12345");
        assertThat(result.get("TRADE_DATE")).isNotNull();
        assertThat(result.get("SETTLE_DATE")).isNotNull();
        assertThat((BigDecimal) result.get("QUANTITY")).isEqualByComparingTo(new BigDecimal("100.5"));
        assertThat((BigDecimal) result.get("PRICE")).isEqualByComparingTo(new BigDecimal("12.34"));
        assertThat((BigDecimal) result.get("PRINCIPAL")).isEqualByComparingTo(new BigDecimal("1240.17"));
        assertThat((BigDecimal) result.get("NET_AMOUNT")).isEqualByComparingTo(new BigDecimal("1250.00"));
    }
}
