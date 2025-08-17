package com.poc.trademanager.repository;

import com.poc.trademanager.entity.AppUser;
import com.poc.trademanager.entity.UserFundEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFundEntitlementRepository extends JpaRepository<UserFundEntitlement, Long> {
    List<UserFundEntitlement> findByUser(AppUser user);
}
