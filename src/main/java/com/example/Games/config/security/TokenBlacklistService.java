package com.example.Games.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory token blacklist service.
 * In production, you might want to use Redis or a database-backed solution.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    private final JwtService jwtService;

    public TokenBlacklistService(JwtService jwtService) {
        this.jwtService = jwtService;
        
        // Clean up expired tokens every hour
        cleanupExecutor.scheduleAtFixedRate(this::removeExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Add a token to the blacklist (for logout functionality)
     */
    public void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.add(token.trim());
            log.debug("Token blacklisted: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
        }
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        return token != null && blacklistedTokens.contains(token.trim());
    }

    /**
     * Remove expired tokens from the blacklist to prevent memory leaks
     */
    private void removeExpiredTokens() {
        int initialSize = blacklistedTokens.size();
        
        blacklistedTokens.removeIf(token -> {
            try {
                return jwtService.isTokenExpired(token);
            } catch (Exception e) {
                // If we can't parse the token, consider it expired and remove it
                return true;
            }
        });
        
        int removedCount = initialSize - blacklistedTokens.size();
        if (removedCount > 0) {
            log.info("Cleaned up {} expired tokens from blacklist. Current size: {}", removedCount, blacklistedTokens.size());
        }
    }

    /**
     * Get the current number of blacklisted tokens (for monitoring)
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    /**
     * Clear all blacklisted tokens (for testing or emergency situations)
     */
    public void clearBlacklist() {
        blacklistedTokens.clear();
        log.warn("Token blacklist cleared");
    }
}
