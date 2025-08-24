package com.poc.trademanager.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "funds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fund {

    @Id
    @Column(length = 20)
    private String fundID;

    @Column(length = 100)
    private String fundName;

    @Column(length = 10)
    private String fundTicker;

    @Column(length = 12)
    private String isin;

    @Column(length = 50)
    private String fundType;

    @Column(length = 50)
    private String legalStructure;

    @Column(length = 50)
    private String domicile;

    private LocalDate inceptionDate;

    @Column(length = 5)
    private String fiscalYearEnd;

    @Column(length = 3)
    private String baseCurrency;

    @Column(precision = 5, scale = 2)
    private BigDecimal managementFee;

    @Column(precision = 5, scale = 2)
    private BigDecimal performanceFee;

    @Column(length = 100)
    private String fundAdministrator;

    @Column(length = 100)
    private String custodian;

    @Lob
    private String primeBrokers;

    @Lob
    private String investmentStrategy;

    @Column(length = 20)
    private String valuationFrequency;

    @Column(length = 20)
    private String subscriptionCycle;

    @Column(length = 20)
    private String redemptionCycle;

    @Column(precision = 18, scale = 6)
    private BigDecimal nav;

    private LocalDate navDate;

    @Column(length = 20)
    private String status;

    @Column(length = 50)
    private String lastUpdatedBy;

    private LocalDateTime lastUpdatedDate;
}
