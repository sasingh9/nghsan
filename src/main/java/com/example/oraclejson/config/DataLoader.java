package com.example.oraclejson.config;

import com.example.oraclejson.entity.AppUser;
import com.example.oraclejson.entity.UserFundEntitlement;
import com.example.oraclejson.repository.AppUserRepository;
import com.example.oraclejson.repository.UserFundEntitlementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
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
    }
}
