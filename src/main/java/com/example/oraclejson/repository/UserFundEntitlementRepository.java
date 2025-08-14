package com.example.oraclejson.repository;

import com.example.oraclejson.entity.AppUser;
import com.example.oraclejson.entity.UserFundEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFundEntitlementRepository extends JpaRepository<UserFundEntitlement, Long> {
    List<UserFundEntitlement> findByUser(AppUser user);
}
