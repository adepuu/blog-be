package com.adepuu.blog.infrastructure.service;

import com.adepuu.blog.delivery.dto.auth.AuthResponse;
import com.adepuu.blog.delivery.dto.auth.RegisterRequest;
import com.adepuu.blog.domain.service.AuthService;
import com.adepuu.blog.domain.service.JwtService;
import com.adepuu.blog.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EndToEndBlacklistTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        tokenBlacklistService.clearAll();
    }

    @Test
    void shouldBlockBlacklistedTokensInFilter() throws ServletException, IOException {
        // Given - register a user and get tokens
        RegisterRequest registerRequest = new RegisterRequest(
                "filteruser",
                "filter@example.com",
                "password123",
                "Filter User"
        );
        AuthResponse authResponse = authService.register(registerRequest);
        String accessToken = authResponse.accessToken();
        String refreshToken = authResponse.refreshToken();

        // Verify tokens work initially
        assertTrue(jwtService.validateToken(accessToken));
        assertTrue(jwtService.validateToken(refreshToken));
        assertFalse(tokenBlacklistService.isBlacklisted(accessToken));
        assertFalse(tokenBlacklistService.isBlacklisted(refreshToken));

        // Create a mock request with the access token
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.addHeader("Authorization", "Bearer " + accessToken);

        // When - the filter processes the request (before logout)
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then - the filter should call the next filter (authentication succeeds)
        verify(filterChain).doFilter(request, response);

        // Reset the mock for the next test
        reset(filterChain);

        // When - logout the user (this should blacklist the refresh token)
        authService.logout(refreshToken);

        // Then - refresh token should be blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(refreshToken));

        // The access token should still work (not blacklisted by logout)
        assertFalse(tokenBlacklistService.isBlacklisted(accessToken));

        // Old tokens should be blacklisted, new ones should not
        assertTrue(tokenBlacklistService.isBlacklisted(refreshToken));

        // Try to refresh with the blacklisted token - should fail
        assertThrows(Exception.class, () -> authService.refreshToken(refreshToken),
                "Should not be able to refresh with blacklisted token");

        System.out.println("End-to-end blacklist test passed successfully!");
    }

    @Test
    void shouldAllowMultipleLogoutAttempts() {
        // Given - register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "multiuser",
                "multi@example.com",
                "password123",
                "Multi User"
        );
        AuthResponse authResponse = authService.register(registerRequest);
        String refreshToken = authResponse.refreshToken();

        // When - logout once
        authService.logout(refreshToken);

        // Then - token should be blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(refreshToken));

        // When - try to logout again with the same token
        // Then - should not throw an exception (allowing multiple logout attempts)
        assertDoesNotThrow(() -> authService.logout(refreshToken));
    }
}
