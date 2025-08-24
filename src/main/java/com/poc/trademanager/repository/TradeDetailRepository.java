package com.poc.trademanager.repository;

import com.poc.trademanager.dto.FundTradeCount;
import com.poc.trademanager.entity.TradeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeDetailRepository extends JpaRepository<TradeDetail, Long> {
    boolean existsByClientReferenceNumber(String clientReferenceNumber);
    List<TradeDetail> findByClientReferenceNumber(String clientReferenceNumber);
    List<TradeDetail> findByClientReferenceNumberAndFundNumberIn(String clientReferenceNumber, List<String> fundNumbers);
    List<TradeDetail> findByClientReferenceNumberAndFundNumberInAndTradeDateBetween(String clientReferenceNumber, List<String> fundNumbers, LocalDateTime startDate, LocalDateTime endDate);
    List<TradeDetail> findByFundNumberInAndTradeDateBetween(List<String> fundNumbers, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT new com.poc.trademanager.dto.FundTradeCount(t.fundNumber, COUNT(t)) FROM TradeDetail t GROUP BY t.fundNumber")
    List<FundTradeCount> countByFundNumber();
}
