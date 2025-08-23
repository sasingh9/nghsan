package com.poc.trademanager.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundTradeCount {
    private String fundNumber;
    private long count;
}
