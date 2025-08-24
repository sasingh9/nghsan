package com.poc.trademanager.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trade_details")
public class TradeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_reference_number", unique = true, nullable = false)
    private String clientReferenceNumber;

    @Column(name = "fund_number")
    private String fundNumber;

    @Column(name = "security_id")
    private String securityId;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(name = "settle_date")
    private LocalDate settleDate;

    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal principal;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
