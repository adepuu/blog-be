package com.adepuu.blog.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    @Test
    void shouldBlacklistValidToken() {
        // Given
        String token = "valid.jwt.token";
        long futureExpiration = System.currentTimeMillis() + 60000; // 1 minute from now
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.blacklistToken(token, futureExpiration);

        // Then
        verify(valueOperations).set(eq("blacklist:token:" + token), eq(futureExpiration), any(Duration.class));
        verify(valueOperations).increment("blacklist:count");
    }

    @Test
    void shouldNotBlacklistExpiredToken() {
        // Given
        String token = "expired.jwt.token";
        long pastExpiration = System.currentTimeMillis() - 60000; // 1 minute ago
        // No need to mock redisTemplate since the method should return early

        // When
        tokenBlacklistService.blacklistToken(token, pastExpiration);

        // Then
        // No Redis operations should have been called
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldReturnTrueForBlacklistedToken() {
        // Given
        String token = "blacklisted.jwt.token";
        when(redisTemplate.hasKey("blacklist:token:" + token)).thenReturn(true);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseForNonBlacklistedToken() {
        // Given
        String token = "clean.jwt.token";
        when(redisTemplate.hasKey("blacklist:token:" + token)).thenReturn(false);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldReturnTrueOnRedisFailureForSecurityReasons() {
        // Given
        String token = "unknown.jwt.token";
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Then
        assertTrue(result, "Should fail securely by rejecting token when Redis is unavailable");
    }

    @Test
    void shouldRemoveSpecificToken() {
        // Given
        String token = "token.to.remove";
        when(redisTemplate.delete("blacklist:token:" + token)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.removeToken(token);

        // Then
        verify(redisTemplate).delete("blacklist:token:" + token);
        verify(valueOperations).decrement("blacklist:count");
    }

    @Test
    void shouldReturnCorrectTokenCount() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blacklist:count")).thenReturn(42L);

        // When
        long count = tokenBlacklistService.getBlacklistedTokenCount();

        // Then
        assertEquals(42L, count);
    }

    @Test
    void shouldReturnZeroWhenCountIsNull() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blacklist:count")).thenReturn(null);

        // When
        long count = tokenBlacklistService.getBlacklistedTokenCount();

        // Then
        assertEquals(0L, count);
    }

    @Test
    void shouldReturnNegativeOneOnCountError() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blacklist:count")).thenThrow(new RuntimeException("Redis error"));

        // When
        long count = tokenBlacklistService.getBlacklistedTokenCount();

        // Then
        assertEquals(-1L, count);
    }

    @Test
    void shouldClearAllTokens() {
        // Given
        Set<String> mockKeys = Set.of(
            "blacklist:token:token1",
            "blacklist:token:token2",
            "blacklist:token:token3"
        );
        when(redisTemplate.keys("blacklist:token:*")).thenReturn(mockKeys);

        // When
        tokenBlacklistService.clearAll();

        // Then
        verify(redisTemplate).delete(mockKeys);
        verify(redisTemplate).delete("blacklist:count");
    }

    @Test
    void shouldReturnTrueForHealthyRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenReturn(null);

        // When
        boolean healthy = tokenBlacklistService.isHealthy();

        // Then
        assertTrue(healthy);
    }

    @Test
    void shouldReturnFalseForUnhealthyRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenThrow(new RuntimeException("Connection failed"));

        // When
        boolean healthy = tokenBlacklistService.isHealthy();

        // Then
        assertFalse(healthy);
    }

    @Test
    void shouldThrowExceptionOnBlacklistFailure() {
        // Given
        String token = "problematic.token";
        long futureExpiration = System.currentTimeMillis() + 60000;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis write failed"))
            .when(valueOperations).set(anyString(), any(), any(Duration.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            tokenBlacklistService.blacklistToken(token, futureExpiration);
        });
    }
}
