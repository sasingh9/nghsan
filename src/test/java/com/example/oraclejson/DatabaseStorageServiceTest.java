package com.example.oraclejson;

import com.example.oraclejson.dto.TradeDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "${app.kafka.topic.json-output}" })
class DatabaseStorageServiceTest {

    @Autowired
    private DatabaseStorageService databaseStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${app.kafka.topic.json-output}")
    private String outputTopic;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singleton(outputTopic));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

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
    void testSaveTradeDetails() throws Exception {
        // Given
        String jsonMessage = "{\"client_reference_number\":\"CLIENT-001\",\"fund_number\":\"FUND-A\",\"security_id\":\"SEC-12345\",\"trade_date\":\"2023-10-26T10:00:00Z\",\"settle_date\":\"2023-10-28T10:00:00Z\",\"quantity\":100.5,\"price\":12.34,\"principal\":1240.17,\"net_amount\":1250.00}";

        // When
        databaseStorageService.save(jsonMessage);

        // Then
        // Verify database insert
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

        // Verify Kafka message
        ConsumerRecord<String, String> received = KafkaTestUtils.getSingleRecord(consumer, outputTopic);
        assertThat(received).isNotNull();
        String receivedValue = received.value();
        TradeDetails receivedTradeDetails = objectMapper.readValue(receivedValue, TradeDetails.class);
        assertThat(receivedTradeDetails.getClientReferenceNumber()).isEqualTo("CLIENT-001");
        assertThat(receivedTradeDetails.getFundNumber()).isEqualTo("FUND-A");
    }
}
