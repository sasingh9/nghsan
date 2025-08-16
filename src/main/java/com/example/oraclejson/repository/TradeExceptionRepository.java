package com.example.oraclejson.repository;

import com.example.oraclejson.entity.TradeException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeExceptionRepository extends JpaRepository<TradeException, Long> {
    boolean existsByClientReferenceNumber(String clientReferenceNumber);
    List<TradeException> findByClientReferenceNumber(String clientReferenceNumber);
}
