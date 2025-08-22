package com.poc.trademanager.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final HeaderValidationInterceptor headerValidationInterceptor;

    @Autowired
    public WebConfig(HeaderValidationInterceptor headerValidationInterceptor) {
        this.headerValidationInterceptor = headerValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(headerValidationInterceptor).addPathPatterns("/api/**")
                .excludePathPatterns("/api/data");
    }
}
