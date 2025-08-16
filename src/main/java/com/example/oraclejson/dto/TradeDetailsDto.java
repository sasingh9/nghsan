package com.example.oraclejson.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TradeDetailsDto {
    private String clientReferenceNumber;
    private String fundNumber;
    private String securityId;
    private Date tradeDate;
    private Date settleDate;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal principal;
    private BigDecimal netAmount;
}
