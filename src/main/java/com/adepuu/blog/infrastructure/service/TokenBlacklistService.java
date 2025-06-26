package com.adepuu.blog.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis-backed token blacklist service for production use.
 * Stores blacklisted tokens with automatic expiration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";
    private static final String BLACKLIST_COUNT_KEY = "blacklist:count";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Blacklist a token
     * @param token The token to blacklist
     * @param expirationTime When the token expires (for automatic cleanup)
     */
    public void blacklistToken(String token, long expirationTime) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            long currentTime = System.currentTimeMillis();
            
            // Only blacklist if token hasn't already expired
            if (expirationTime > currentTime) {
                long ttlSeconds = (expirationTime - currentTime) / 1000;
                
                // Store the token with TTL matching its expiration
                redisTemplate.opsForValue().set(key, expirationTime, Duration.ofSeconds(ttlSeconds));
                
                // Increment counter for monitoring
                redisTemplate.opsForValue().increment(BLACKLIST_COUNT_KEY);
                
                log.debug("Token blacklisted in Redis: {} (TTL: {} seconds)", 
                    token.substring(0, Math.min(token.length(), 10)) + "...", ttlSeconds);
            } else {
                log.debug("Token already expired, not blacklisting: {}", 
                    token.substring(0, Math.min(token.length(), 10)) + "...");
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis", e);
            // In case of Redis failure, we should fail securely by rejecting the token
            throw new RuntimeException("Token blacklist service unavailable", e);
        }
    }
    
    /**
     * Check if a token is blacklisted
     * @param token The token to check
     * @return true if the token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            
            boolean blacklisted = Boolean.TRUE.equals(exists);
            
            if (blacklisted) {
                log.debug("Token found in blacklist: {}", 
                    token.substring(0, Math.min(token.length(), 10)) + "...");
            }
            
            return blacklisted;
        } catch (Exception e) {
            log.error("Failed to check token blacklist in Redis", e);
            // In case of Redis failure, we should fail securely by rejecting the token
            return true;
        }
    }
    
    /**
     * Remove a specific token from blacklist (for testing/admin purposes)
     */
    public void removeToken(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                redisTemplate.opsForValue().decrement(BLACKLIST_COUNT_KEY);
                log.debug("Token removed from blacklist: {}", 
                    token.substring(0, Math.min(token.length(), 10)) + "...");
            }
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist", e);
        }
    }
    
    /**
     * Get the number of blacklisted tokens (for monitoring)
     */
    public long getBlacklistedTokenCount() {
        try {
            Object count = redisTemplate.opsForValue().get(BLACKLIST_COUNT_KEY);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            log.error("Failed to get blacklisted token count", e);
            return -1; // Indicates error
        }
    }
    
    /**
     * Clear all blacklisted tokens (for testing/admin purposes)
     */
    public void clearAll() {
        try {
            Set<String> keys = redisTemplate.keys(BLACKLIST_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                redisTemplate.delete(BLACKLIST_COUNT_KEY);
                log.info("All blacklisted tokens cleared from Redis (count: {})", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear blacklisted tokens", e);
        }
    }
    
    /**
     * Health check for Redis connection
     */
    public boolean isHealthy() {
        try {
            redisTemplate.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            log.warn("Redis health check failed", e);
            return false;
        }
    }
}
