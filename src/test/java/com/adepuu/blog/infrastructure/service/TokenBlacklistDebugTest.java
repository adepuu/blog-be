package com.adepuu.blog.infrastructure.service;

import com.adepuu.blog.delivery.dto.auth.AuthResponse;
import com.adepuu.blog.delivery.dto.auth.RegisterRequest;
import com.adepuu.blog.domain.repository.UserRepository;
import com.adepuu.blog.domain.service.AuthService;
import com.adepuu.blog.domain.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TokenBlacklistDebugTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        userRepository.deleteAll();
        
        // Clear blacklist
        tokenBlacklistService.clearAll();
    }

    @Test
    void debugBlacklistFlow() {
        // Given - register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "debuguser",
                "debug@example.com",
                "password123",
                "Debug User"
        );
        AuthResponse authResponse = authService.register(registerRequest);
        String refreshToken = authResponse.refreshToken();
        
        System.out.println("Generated refresh token: " + refreshToken.substring(0, 20) + "...");
        
        // Test token validation
        boolean isValid = jwtService.validateToken(refreshToken);
        System.out.println("Token is valid: " + isValid);
        
        // Test token expiration extraction
        var expirationTime = jwtService.getExpirationTimeFromToken(refreshToken);
        System.out.println("Token expiration time: " + expirationTime);
        
        // Verify not blacklisted initially
        boolean initiallyBlacklisted = tokenBlacklistService.isBlacklisted(refreshToken);
        System.out.println("Initially blacklisted: " + initiallyBlacklisted);
        assertFalse(initiallyBlacklisted, "Token should not be blacklisted initially");
        
        // Test direct blacklisting with the service
        System.out.println("Testing direct blacklisting...");
        if (expirationTime != null) {
            long expTime = expirationTime.toEpochMilli();
            System.out.println("Directly blacklisting token with expiration: " + expTime);
            tokenBlacklistService.blacklistToken(refreshToken, expTime);
            
            boolean directlyBlacklisted = tokenBlacklistService.isBlacklisted(refreshToken);
            System.out.println("After direct blacklisting: " + directlyBlacklisted);
            long directCount = tokenBlacklistService.getBlacklistedTokenCount();
            System.out.println("Count after direct blacklisting: " + directCount);
            
            // Clear for the actual test
            tokenBlacklistService.clearAll();
        }
        
        // When - logout
        System.out.println("Calling logout...");
        try {
            authService.logout(refreshToken);
            System.out.println("Logout completed without exception");
        } catch (Exception e) {
            System.out.println("Logout threw exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        // Then - check if blacklisted
        boolean afterLogoutBlacklisted = tokenBlacklistService.isBlacklisted(refreshToken);
        System.out.println("After logout blacklisted: " + afterLogoutBlacklisted);
        
        // Also check blacklist count
        long blacklistCount = tokenBlacklistService.getBlacklistedTokenCount();
        System.out.println("Total blacklisted tokens: " + blacklistCount);
        
        assertTrue(afterLogoutBlacklisted, "Token should be blacklisted after logout");
    }
}
