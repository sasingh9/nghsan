package com.poc.trademanager.service;

import com.poc.trademanager.dto.KafkaMessageDto;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final ConsumerFactory<String, String> consumerFactory;

    public KafkaAdminService(ConsumerFactory<String, String> consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    public List<KafkaMessageDto> browseMessages(String topic) {
        Map<String, Object> consumerProps = consumerFactory.getConfigurationProperties();
        Properties props = new Properties();
        props.putAll(consumerProps);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-browser-" + System.currentTimeMillis());

        List<KafkaMessageDto> messages = new ArrayList<>();
        try (Consumer<String, String> consumer = consumerFactory.createConsumer(props.getProperty(ConsumerConfig.GROUP_ID_CONFIG), null, null, props)) {
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
            if (partitionInfos != null) {
                List<TopicPartition> topicPartitions = partitionInfos.stream()
                        .map(info -> new TopicPartition(topic, info.partition()))
                        .collect(Collectors.toList());
                consumer.assign(topicPartitions);
                consumer.seekToBeginning(topicPartitions);

                boolean messagesAvailable = true;
                while (messagesAvailable) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    if (records.isEmpty()) {
                        messagesAvailable = false;
                    } else {
                        records.forEach(record -> messages.add(new KafkaMessageDto(
                                record.timestamp(),
                                record.key(),
                                record.value()
                        )));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error browsing messages for topic {}", topic, e);
            throw new RuntimeException("Error browsing messages from Kafka", e);
        }

        messages.sort(Comparator.comparingLong(KafkaMessageDto::getTimestamp));
        return messages;
    }
}
