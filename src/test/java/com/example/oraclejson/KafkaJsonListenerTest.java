package com.example.oraclejson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "${app.kafka.topic.json-input}" })
class KafkaJsonListenerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private DatabaseStorageService databaseStorageService;

    @Value("${app.kafka.topic.json-input}")
    private String topic;

    @Test
    void testKafkaListenerReceivesMessageAndCallsService() throws Exception {
        // Given
        String jsonMessage = "{\"test_key\":\"test_value\"}";

        // When
        // Add a delay to ensure the consumer is ready before sending a message
        Thread.sleep(1000);
        kafkaTemplate.send(topic, jsonMessage);

        // Then
        // Verify that the save method on the mocked service was called within a certain timeout
        // This makes the test asynchronous and more robust.
        verify(databaseStorageService, timeout(5000).times(1)).save(jsonMessage);
    }
}
