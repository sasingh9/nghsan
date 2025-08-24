package com.poc.trademanager.repository;

import com.poc.trademanager.entity.TradeException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeExceptionRepository extends JpaRepository<TradeException, Long> {
    boolean existsByClientReferenceNumber(String clientReferenceNumber);
    List<TradeException> findByClientReferenceNumber(String clientReferenceNumber);
    List<TradeException> findByClientReferenceNumberAndCreatedAtBetween(String clientReferenceNumber, LocalDateTime startDate, LocalDateTime endDate);
    List<TradeException> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
