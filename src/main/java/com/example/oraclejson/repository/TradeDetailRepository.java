package com.example.oraclejson.repository;

import com.example.oraclejson.entity.TradeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeDetailRepository extends JpaRepository<TradeDetail, Long> {
    boolean existsByClientReferenceNumber(String clientReferenceNumber);
    List<TradeDetail> findByClientReferenceNumber(String clientReferenceNumber);
    List<TradeDetail> findByClientReferenceNumberAndFundNumberIn(String clientReferenceNumber, List<String> fundNumbers);
}
