package com.theagilemonkeys.crm.service;

import com.theagilemonkeys.crm.dto.ComponentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@Service
public class HealthCheckService {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public ComponentStatus checkDatabaseStatus() {
        if (dataSource == null) {
            return new ComponentStatus("database", "UNKNOWN");
        }
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return new ComponentStatus("database", "UP");
            } else {
                return new ComponentStatus("database", "DOWN");
            }
        } catch (Exception e) {
            return new ComponentStatus("database", "DOWN");
        }
    }

    public ComponentStatus checkKafkaStatus() {
        try {
            Map<String, ?> producerMetrics = kafkaTemplate.metrics();
            if (producerMetrics != null && !producerMetrics.isEmpty()) {
                return new ComponentStatus("kafka", "UP");
            } else {
                return new ComponentStatus("kafka", "DOWN");
            }
        } catch (Exception e) {
            return new ComponentStatus("kafka", "DOWN");
        }
    }
}
