package com.poc.trademanager.repository;

import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.entity.JsonDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JsonDocRepository extends JpaRepository<JsonDoc, Long> {
    Page<JsonDoc> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<JsonDoc> findByCreatedAtGreaterThanEqual(LocalDateTime start, Pageable pageable);
    Page<JsonDoc> findByCreatedAtLessThanEqual(LocalDateTime end, Pageable pageable);
}
