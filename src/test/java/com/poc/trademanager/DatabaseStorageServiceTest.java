package com.poc.trademanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.repository.AppUserRepository;
import com.poc.trademanager.repository.JsonDocRepository;
import com.poc.trademanager.repository.TradeDetailRepository;
import com.poc.trademanager.repository.TradeExceptionRepository;
import com.poc.trademanager.repository.UserFundEntitlementRepository;
import com.poc.trademanager.service.UniqueIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DatabaseStorageServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JsonDocRepository jsonDocRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private UserFundEntitlementRepository userFundEntitlementRepository;
    @Autowired
    private TradeDetailRepository tradeDetailRepository;
    @Autowired
    private TradeExceptionRepository tradeExceptionRepository;

    private DatabaseStorageService databaseStorageService;

    @BeforeEach
    void setUp() {
        UniqueIdGenerator uniqueIdGenerator = new UniqueIdGenerator();
        uniqueIdGenerator.init();
        ObjectMapper objectMapper = new ObjectMapper();
        databaseStorageService = new DatabaseStorageService(
                appUserRepository,
                userFundEntitlementRepository,
                uniqueIdGenerator,
                jsonDocRepository,
                tradeDetailRepository,
                tradeExceptionRepository,
                objectMapper
        );
    }

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
