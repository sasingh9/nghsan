package com.poc.trademanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private String baseCurrency;
    private String outboundJson;
}
