package com.poc.trademanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessageDto {
    private long timestamp;
    private String key;
    private String value;
}
