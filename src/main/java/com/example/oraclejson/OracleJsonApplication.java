package com.example.oraclejson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableMethodSecurity(prePostEnabled = true)
public class OracleJsonApplication {

    public static void main(String[] args) {
        SpringApplication.run(OracleJsonApplication.class, args);
    }

}
