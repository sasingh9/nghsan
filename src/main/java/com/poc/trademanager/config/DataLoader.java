package com.poc.trademanager.config;

import com.poc.trademanager.entity.AppUser;
import com.poc.trademanager.entity.UserFundEntitlement;
import com.poc.trademanager.repository.AppUserRepository;
import com.poc.trademanager.repository.UserFundEntitlementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
@Profile("!prod") // Only run this in non-production environments
public class DataLoader implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final UserFundEntitlementRepository userFundEntitlementRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(AppUserRepository appUserRepository, UserFundEntitlementRepository userFundEntitlementRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.userFundEntitlementRepository = userFundEntitlementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create a test user if one doesn't exist
        if (appUserRepository.findByUsername("user").isEmpty()) {
            AppUser user = new AppUser();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("password"));
            user.setRoles("ROLE_USER");
            appUserRepository.save(user);

            // Give the user entitlements to some funds
            UserFundEntitlement entitlement1 = new UserFundEntitlement();
            entitlement1.setUser(user);
            entitlement1.setFundNumber("FUND-A");

            UserFundEntitlement entitlement2 = new UserFundEntitlement();
            entitlement2.setUser(user);
            entitlement2.setFundNumber("FUND-B");

            userFundEntitlementRepository.saveAll(List.of(entitlement1, entitlement2));
        }

        if (appUserRepository.findByUsername("supportuser").isEmpty()) {
            AppUser supportUser = new AppUser();
            supportUser.setUsername("supportuser");
            supportUser.setPassword(passwordEncoder.encode("password"));
            supportUser.setRoles("ROLE_SUPPORT");
            appUserRepository.save(supportUser);
        }
    }
}
