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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TokenBlacklistIntegrationTest {

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
    void shouldRegisterAndLoginSuccessfully() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest(
                "newuser",
                "newuser@example.com",
                "password123",
                "New User"
        );

        // When
        AuthResponse registerResponse = authService.register(registerRequest);
        
        // Then
        assertNotNull(registerResponse);
        assertNotNull(registerResponse.accessToken());
        assertNotNull(registerResponse.refreshToken());
        assertEquals("newuser", registerResponse.user().getUsername());

        // Verify tokens are valid
        assertTrue(jwtService.validateToken(registerResponse.accessToken()));
        assertTrue(jwtService.validateToken(registerResponse.refreshToken()));
    }

    @Test
    void shouldCallBlacklistServiceOnLogout() {
        // Given - register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "logoutuser",
                "logout@example.com",
                "password123",
                "Logout User"
        );
        AuthResponse authResponse = authService.register(registerRequest);
        String refreshToken = authResponse.refreshToken();

        // When - logout
        authService.logout(refreshToken);

        // Then - token should be blacklisted (we can test this by checking the blacklist)
        assertTrue(tokenBlacklistService.isBlacklisted(refreshToken));
    }

    @Test
    void shouldRejectBlacklistedRefreshToken() {
        // Given - register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "refreshuser",
                "refresh@example.com",
                "password123",
                "Refresh User"
        );
        AuthResponse authResponse = authService.register(registerRequest);
        String refreshToken = authResponse.refreshToken();

        // First logout to blacklist the token
        authService.logout(refreshToken);

        // When & Then - trying to refresh with blacklisted token should fail
        assertThrows(BadCredentialsException.class, () -> {
            authService.refreshToken(refreshToken);
        });
    }

    @Test
    void shouldBlacklistOldTokenOnRefresh() {
        // Given - register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "tokenuser",
                "token@example.com",
                "password123",
                "Token User"
        );
        AuthResponse authResponse = authService.register(registerRequest);
        String originalRefreshToken = authResponse.refreshToken();

        // When - refresh the token
        AuthResponse refreshResponse = authService.refreshToken(originalRefreshToken);

        // Then - old token should be blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(originalRefreshToken));
        
        // New tokens should be different and valid
        assertNotEquals(originalRefreshToken, refreshResponse.refreshToken());
        assertTrue(jwtService.validateToken(refreshResponse.accessToken()));
        assertTrue(jwtService.validateToken(refreshResponse.refreshToken()));
    }

    @Test
    void shouldValidateRefreshTokenBeforeBlacklisting() {
        // Given - invalid refresh token
        String invalidToken = "invalid.jwt.token";

        // When & Then - should fail validation before checking blacklist
        assertThrows(BadCredentialsException.class, () -> {
            authService.logout(invalidToken);
        });
    }
}
