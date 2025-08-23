package com.poc.trademanager.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeSummaryDto {
    private String fundNumber;
    private long tradesReceived;
    private long tradesCreated;
    private long exceptions;
}
