package com.example.Games.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Skip logging for health checks and static resources
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Generate or use existing correlation ID
        String correlationId = getOrCreateCorrelationId(request);
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        
        try {
            // Log request
            logRequest(wrappedRequest, correlationId);
            
            // Process request
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log response
            logResponse(wrappedResponse, correlationId, duration);
            
            // Important: Copy cached content to response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private void logRequest(ContentCachingRequestWrapper request, String correlationId) {
        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String remoteAddr = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("REQUEST [{}] {} {} {} - User-Agent: {} - IP: {}", 
                    correlationId, method, uri, 
                    queryString != null ? "?" + queryString : "", 
                    userAgent, remoteAddr);

            // Log request body for POST/PUT/PATCH (excluding sensitive endpoints)
            if (shouldLogRequestBody(request)) {
                byte[] content = request.getContentAsByteArray();
                if (content.length > 0) {
                    String contentStr = new String(content);
                    // Don't log passwords or sensitive data
                    if (!containsSensitiveData(uri, contentStr)) {
                        log.debug("REQUEST_BODY [{}]: {}", correlationId, contentStr);
                    } else {
                        log.debug("REQUEST_BODY [{}]: <sensitive data hidden>", correlationId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error logging request [{}]: {}", correlationId, e.getMessage());
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, String correlationId, long duration) {
        try {
            int status = response.getStatus();
            String contentType = response.getContentType();
            
            log.info("RESPONSE [{}] {} {} - Duration: {}ms", 
                    correlationId, status, contentType != null ? contentType : "unknown", duration);

            // Log response body for errors or debug level
            if (log.isDebugEnabled() || status >= 400) {
                byte[] content = response.getContentAsByteArray();
                if (content.length > 0 && content.length < 1024) { // Limit size
                    String contentStr = new String(content);
                    log.debug("RESPONSE_BODY [{}]: {}", correlationId, contentStr);
                }
            }
        } catch (Exception e) {
            log.warn("Error logging response [{}]: {}", correlationId, e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", 
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs (X-Forwarded-For can contain multiple IPs)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/health") ||
               uri.startsWith("/favicon.ico") ||
               uri.startsWith("/static/") ||
               uri.startsWith("/css/") ||
               uri.startsWith("/js/") ||
               uri.startsWith("/images/");
    }

    private boolean shouldLogRequestBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private boolean containsSensitiveData(String uri, String content) {
        String lowerUri = uri.toLowerCase();
        String lowerContent = content.toLowerCase();
        
        // Check for sensitive endpoints
        if (lowerUri.contains("/auth/") || lowerUri.contains("/login") || lowerUri.contains("/register")) {
            return true;
        }
        
        // Check for sensitive fields in content
        return lowerContent.contains("\"password\"") ||
               lowerContent.contains("\"token\"") ||
               lowerContent.contains("\"secret\"") ||
               lowerContent.contains("\"key\"");
    }
}
