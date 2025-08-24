package com.poc.trademanager;

import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.service.MessageProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class KafkaJsonListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaJsonListener.class);

    private final DatabaseStorageService databaseStorageService;
    private final MessageProcessingService messageProcessingService;

    public KafkaJsonListener(DatabaseStorageService databaseStorageService, MessageProcessingService messageProcessingService) {
        this.databaseStorageService = databaseStorageService;
        this.messageProcessingService = messageProcessingService;
    }

    @KafkaListener(topics = "${app.kafka.topic.json-input}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message, Acknowledgment acknowledgment) {
        try {
            logger.info("Received message: {}", message);
            JsonDoc rawMessage = databaseStorageService.saveRawMessage(message);
            messageProcessingService.processMessage(rawMessage);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            // Decide on error handling, e.g., send to a dead-letter topic
        }
    }
}
