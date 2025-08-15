package com.example.oraclejson;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaJsonListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaJsonListener.class);

    private final DatabaseStorageService storageService;

    public KafkaJsonListener(DatabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @KafkaListener(topics = "${app.kafka.topic.json-input}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(String message) {
        log.info("Received message from Kafka topic: {}", message);
        try {
            storageService.save(message);
        } catch (Exception e) {
            log.error("An error occurred while processing the message. Skipping message: {}", message, e);
            // Catch all exceptions to prevent retries and skip the poison pill message.
        }
    }
}
