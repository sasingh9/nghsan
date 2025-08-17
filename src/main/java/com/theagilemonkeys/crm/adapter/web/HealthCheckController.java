package com.theagilemonkeys.crm.adapter.web;

import com.theagilemonkeys.crm.dto.ComponentStatus;
import com.theagilemonkeys.crm.dto.HealthStatus;
import com.theagilemonkeys.crm.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @Autowired
    private HealthCheckService healthCheckService;

    @GetMapping
    public ResponseEntity<HealthStatus> getHealthStatus() {
        List<ComponentStatus> components = new ArrayList<>();
        ComponentStatus dbStatus = healthCheckService.checkDatabaseStatus();
        ComponentStatus kafkaStatus = healthCheckService.checkKafkaStatus();
        components.add(dbStatus);
        components.add(kafkaStatus);

        String overallStatus = "UP";
        for (ComponentStatus component : components) {
            if ("DOWN".equals(component.getStatus())) {
                overallStatus = "DOWN";
                break;
            }
        }

        HealthStatus healthStatus = new HealthStatus(overallStatus, components);
        return ResponseEntity.ok(healthStatus);
    }
}
