package com.example.oraclejson;

import com.example.oraclejson.entity.JsonDoc;
import com.example.oraclejson.service.MessageProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class KafkaJsonListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaJsonListener.class);

    private final DatabaseStorageService storageService;
    private final MessageProcessingService messageProcessingService;

    public KafkaJsonListener(DatabaseStorageService storageService, MessageProcessingService messageProcessingService) {
        this.storageService = storageService;
        this.messageProcessingService = messageProcessingService;
    }

    @KafkaListener(topics = "${app.kafka.topic.json-input}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(String message, Acknowledgment acknowledgment) {
        log.info("Received message from Kafka topic.");
        long startTime = System.currentTimeMillis();
        try {
            JsonDoc jsonDoc = storageService.saveRawMessage(message);
            acknowledgment.acknowledge();
            long endTime = System.currentTimeMillis();
            log.info("Successfully saved raw message with key {} to the database. Time taken: {} ms", jsonDoc.getMessageKey(), (endTime - startTime));
            messageProcessingService.processMessage(jsonDoc);
        } catch (Exception e) {
            log.error("An error occurred while saving the raw message. The message will be nacked and retried. Message: {}", message, e);
            // Not acknowledging the message, so it will be redelivered.
        }
    }
}
