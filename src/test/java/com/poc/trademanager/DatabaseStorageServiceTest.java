package com.poc.trademanager;

import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.repository.JsonDocRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestEntityManager
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.kafka.topic.json-input=test-input-topic",
        "app.kafka.topic.json-output=test-output-topic",
        "spring.kafka.consumer.group-id=test-group"
})
class DatabaseStorageServiceTest {

    @Autowired
    private DatabaseStorageService databaseStorageService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JsonDocRepository jsonDocRepository;

    @Test
    @Transactional
    void testSaveRawMessage() {
        // Given
        String jsonMessage = "{\"test_key\":\"test_value\"}";

        // When
        JsonDoc savedDoc = databaseStorageService.saveRawMessage(jsonMessage);
        entityManager.flush();
        entityManager.clear();

        // Then
        JsonDoc foundDoc = jsonDocRepository.findById(savedDoc.getId()).orElse(null);
        assertThat(foundDoc).isNotNull();
        assertThat(foundDoc.getData()).isEqualTo(jsonMessage);
        assertThat(foundDoc.getMessageKey()).isNotNull();
    }
}
