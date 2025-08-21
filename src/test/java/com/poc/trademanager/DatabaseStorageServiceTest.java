package com.poc.trademanager;

import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.repository.JsonDocRepository;
import com.poc.trademanager.service.UniqueIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({DatabaseStorageService.class, UniqueIdGenerator.class})
class DatabaseStorageServiceTest {

    @Autowired
    private DatabaseStorageService databaseStorageService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JsonDocRepository jsonDocRepository;

    @Test
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
