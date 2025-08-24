package com.poc.trademanager.controller;

import com.poc.trademanager.dto.ApiResponse;
import com.poc.trademanager.dto.KafkaMessageDto;
import com.poc.trademanager.service.KafkaAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @GetMapping("/browse/{topicName}")
    @PreAuthorize("hasRole('SUPPORT')")
    public ResponseEntity<ApiResponse<List<KafkaMessageDto>>> browseTopic(@PathVariable String topicName) {
        List<KafkaMessageDto> messages = kafkaAdminService.browseMessages(topicName);
        return ResponseEntity.ok(new ApiResponse<>(true, "Messages browsed successfully", messages));
    }
}
