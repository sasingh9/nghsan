package com.poc.trademanager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TradeDetailsDto {
    private String clientReferenceNumber;
    private String fundNumber;
    private String securityId;
    private LocalDate tradeDate;
    private LocalDate settleDate;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal principal;
    private BigDecimal netAmount;
}
