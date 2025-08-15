package com.example.oraclejson.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class HeaderValidationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required header: X-Correlation-ID");
            return false;
        }

        String sourceAppId = request.getHeader("X-Source-Application-ID");
        if (sourceAppId == null || sourceAppId.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required header: X-Source-Application-ID");
            return false;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required header: Authorization");
            return false;
        }

        return true;
    }
}
