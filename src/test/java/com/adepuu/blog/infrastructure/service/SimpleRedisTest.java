package com.adepuu.blog.infrastructure.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SimpleRedisTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void testRedisBasicOperations() {
        // Test basic Redis operations
        String testKey = "test:key";
        String testValue = "test_value";
        
        // Set a value
        redisTemplate.opsForValue().set(testKey, testValue);
        
        // Get the value
        Object retrievedValue = redisTemplate.opsForValue().get(testKey);
        assertEquals(testValue, retrievedValue);
        
        // Check if key exists
        Boolean exists = redisTemplate.hasKey(testKey);
        assertTrue(exists);
        
        // Delete the key
        redisTemplate.delete(testKey);
        
        // Verify it's gone
        Boolean existsAfterDelete = redisTemplate.hasKey(testKey);
        assertFalse(existsAfterDelete);
        
        System.out.println("Redis basic operations work correctly");
    }

    @Test
    void testTokenBlacklistService() {
        // Clear any existing data
        tokenBlacklistService.clearAll();
        
        // Test blacklisting a token
        String testToken = "test.jwt.token";
        long expirationTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        
        System.out.println("Before blacklisting:");
        System.out.println("Is blacklisted: " + tokenBlacklistService.isBlacklisted(testToken));
        System.out.println("Count: " + tokenBlacklistService.getBlacklistedTokenCount());
        
        // Blacklist the token
        tokenBlacklistService.blacklistToken(testToken, expirationTime);
        
        System.out.println("After blacklisting:");
        System.out.println("Is blacklisted: " + tokenBlacklistService.isBlacklisted(testToken));
        System.out.println("Count: " + tokenBlacklistService.getBlacklistedTokenCount());
        
        // Verify it's blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(testToken));
        assertEquals(1, tokenBlacklistService.getBlacklistedTokenCount());
        
        // Clean up
        tokenBlacklistService.clearAll();
    }
}
