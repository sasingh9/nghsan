package com.poc.trademanager;

import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.service.MessageProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "${app.kafka.topic.json-input}" })
class KafkaJsonListenerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private DatabaseStorageService databaseStorageService;

    @MockBean
    private MessageProcessingService messageProcessingService;

    @Value("${app.kafka.topic.json-input}")
    private String topic;

    @TestConfiguration
    static class KafkaTestListenerConfig {
        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, String> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
            return factory;
        }
    }

    @Test
    void testKafkaListenerReceivesMessageAndCallsServices() {
        // Given
        String jsonMessage = "{\"test_key\":\"test_value\"}";
        JsonDoc jsonDoc = new JsonDoc();
        jsonDoc.setId(1L);
        jsonDoc.setMessageKey("test-key");
        jsonDoc.setData(jsonMessage);

        given(databaseStorageService.saveRawMessage(any(String.class))).willReturn(jsonDoc);

        // When
        kafkaTemplate.send(topic, jsonMessage);

        // Then
        verify(databaseStorageService, timeout(5000).times(1)).saveRawMessage(jsonMessage);
        verify(messageProcessingService, timeout(5000).times(1)).processMessage(jsonDoc);
    }
}
