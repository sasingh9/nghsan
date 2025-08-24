package com.poc.trademanager.repository;

import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.entity.JsonDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JsonDocRepository extends JpaRepository<JsonDoc, Long> {
    @Query("SELECT j FROM JsonDoc j WHERE " +
            "(:startDate IS NULL OR j.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR j.createdAt <= :endDate) AND " +
            "(:contentFilter IS NULL OR j.data LIKE %:contentFilter%)")
    Page<JsonDoc> findByCriteria(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("contentFilter") String contentFilter,
            Pageable pageable
    );
}
