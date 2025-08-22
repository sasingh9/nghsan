package com.poc.trademanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Configuration
@Profile("!test")
public class DatabaseInitializer {

    @Value("${app.db.ddl.create-table}")
    private String createTableDdl;

    @Value("${app.db.ddl.create-trade-details-table}")
    private String createTradeDetailsTableDdl;

    @Value("${app.db.ddl.create-trade-exceptions-table}")
    private String createTradeExceptionsTableDdl;

    @Value("${app.db.ddl.create-user-table}")
    private String createUserTableDdl;

    @Value("${app.db.ddl.create-entitlements-table}")
    private String createEntitlementsTableDdl;


    @Bean
    public CommandLineRunner initializeDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute(createUserTableDdl);
            jdbcTemplate.execute(createEntitlementsTableDdl);
            jdbcTemplate.execute(createTableDdl);
            jdbcTemplate.execute(createTradeDetailsTableDdl);
            jdbcTemplate.execute(createTradeExceptionsTableDdl);
        };
    }
}
