package com.adepuu.blog.infrastructure.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void shouldConnectToRedis() {
        // Test basic Redis connectivity
        redisTemplate.opsForValue().set("test:key", "test:value");
        Object value = redisTemplate.opsForValue().get("test:key");
        assertEquals("test:value", value);
        
        // Clean up
        redisTemplate.delete("test:key");
    }

    @Test
    void shouldBlacklistAndCheckToken() {
        // Given
        String testToken = "test.jwt.token.for.blacklist";
        long futureExpiration = System.currentTimeMillis() + 60000; // 1 minute from now
        
        // Verify not blacklisted initially
        assertFalse(tokenBlacklistService.isBlacklisted(testToken), "Token should not be blacklisted initially");
        
        // When - blacklist the token
        tokenBlacklistService.blacklistToken(testToken, futureExpiration);
        
        // Then - should be blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(testToken), "Token should be blacklisted after blacklisting");
        
        // Clean up
        tokenBlacklistService.removeToken(testToken);
        assertFalse(tokenBlacklistService.isBlacklisted(testToken), "Token should not be blacklisted after removal");
    }

    @Test
    void shouldCheckHealthy() {
        assertTrue(tokenBlacklistService.isHealthy(), "Redis should be healthy");
    }
}
