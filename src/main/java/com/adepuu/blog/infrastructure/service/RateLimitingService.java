package com.adepuu.blog.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RateLimitingService {
    
    private final ConcurrentMap<String, RateLimitEntry> cache = new ConcurrentHashMap<>();
    
    // Rate limit configurations
    private static final int POSTS_PER_HOUR = 10;
    private static final int COMMENTS_PER_HOUR = 50;
    private static final int REACTIONS_PER_HOUR = 200;
    private static final int AUTH_ATTEMPTS_PER_HOUR = 5;
    private static final int GENERAL_REQUESTS_PER_MINUTE = 60;
    
    /**
     * Check if a post creation is allowed for the user
     */
    public boolean isPostCreationAllowed(String userId) {
        String key = "post_creation:" + userId;
        return checkRateLimit(key, POSTS_PER_HOUR, Duration.ofHours(1));
    }
    
    /**
     * Check if a comment creation is allowed for the user
     */
    public boolean isCommentCreationAllowed(String userId) {
        String key = "comment_creation:" + userId;
        return checkRateLimit(key, COMMENTS_PER_HOUR, Duration.ofHours(1));
    }
    
    /**
     * Check if a reaction is allowed for the user
     */
    public boolean isReactionAllowed(String userId) {
        String key = "reaction:" + userId;
        return checkRateLimit(key, REACTIONS_PER_HOUR, Duration.ofHours(1));
    }
    
    /**
     * Check if an authentication attempt is allowed for the identifier (username/email/IP)
     */
    public boolean isAuthAttemptAllowed(String identifier) {
        String key = "auth:" + identifier;
        return checkRateLimit(key, AUTH_ATTEMPTS_PER_HOUR, Duration.ofHours(1));
    }
    
    /**
     * Check general request rate limit by IP
     */
    public boolean isGeneralRequestAllowed(String ipAddress) {
        String key = "general:" + ipAddress;
        return checkRateLimit(key, GENERAL_REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
    }
    
    /**
     * Check if user follow/unfollow is allowed
     */
    public boolean isFollowActionAllowed(String userId) {
        String key = "follow:" + userId;
        return checkRateLimit(key, 20, Duration.ofHours(1)); // 20 follow actions per hour
    }
    
    /**
     * Check if a search request is allowed
     */
    public boolean isSearchAllowed(String userId) {
        String key = "search:" + userId;
        return checkRateLimit(key, 30, Duration.ofMinutes(1)); // 30 searches per minute
    }
    
    /**
     * Check if file upload is allowed
     */
    public boolean isFileUploadAllowed(String userId) {
        String key = "upload:" + userId;
        return checkRateLimit(key, 10, Duration.ofHours(1)); // 10 uploads per hour
    }
    
    /**
     * Core rate limiting logic
     */
    private boolean checkRateLimit(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        
        RateLimitEntry entry = cache.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart.plus(window))) {
                // New window or expired window
                return new RateLimitEntry(now, new AtomicInteger(1));
            } else {
                // Within current window
                existing.requestCount.incrementAndGet();
                return existing;
            }
        });
        
        boolean allowed = entry.requestCount.get() <= maxRequests;
        
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}, requests: {}, limit: {}", 
                key, entry.requestCount.get(), maxRequests);
        }
        
        return allowed;
    }
    
    /**
     * Get remaining requests for a specific key
     */
    public int getRemainingRequests(String key, int maxRequests, Duration window) {
        RateLimitEntry entry = cache.get(key);
        if (entry == null || Instant.now().isAfter(entry.windowStart.plus(window))) {
            return maxRequests;
        }
        return Math.max(0, maxRequests - entry.requestCount.get());
    }
    
    /**
     * Reset rate limit for a specific key (useful for testing or admin actions)
     */
    public void resetRateLimit(String key) {
        cache.remove(key);
        log.info("Rate limit reset for key: {}", key);
    }
    
    /**
     * Clear all rate limiting data (useful for cleanup)
     */
    public void clearAll() {
        cache.clear();
        log.info("All rate limiting data cleared");
    }
    
    /**
     * Get cache statistics
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Rate limit entry to store window start time and request count
     */
    private static class RateLimitEntry {
        private final Instant windowStart;
        private final AtomicInteger requestCount;
        
        public RateLimitEntry(Instant windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}
