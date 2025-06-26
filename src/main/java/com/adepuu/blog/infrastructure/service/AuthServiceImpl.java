package com.adepuu.blog.infrastructure.service;

import com.adepuu.blog.delivery.dto.auth.AuthResponse;
import com.adepuu.blog.delivery.dto.auth.LoginRequest;
import com.adepuu.blog.delivery.dto.auth.RegisterRequest;
import com.adepuu.blog.domain.entity.User;
import com.adepuu.blog.domain.repository.UserRepository;
import com.adepuu.blog.domain.service.AuthService;
import com.adepuu.blog.domain.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is disabled");
        }
        
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        log.info("User {} logged in successfully", user.getUsername());
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                user
        );
    }
    
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create new user
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(User.UserRole.USER)
                .emailVerified(false)
                .isActive(true)
                .build();
        
        user = userRepository.save(user);
        
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        log.info("New user registered: {}", user.getUsername());
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                user
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        // Check if token is blacklisted
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new BadCredentialsException("Token has been revoked");
        }
        
        if (!jwtService.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        String userId = jwtService.getUserIdFromToken(refreshToken);
        User user = userRepository.findActiveById(UUID.fromString(userId))
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        // Blacklist the old refresh token
        try {
            Instant expirationInstant = jwtService.getExpirationTimeFromToken(refreshToken);
            if (expirationInstant != null) {
                long expirationTime = expirationInstant.toEpochMilli();
                tokenBlacklistService.blacklistToken(refreshToken, expirationTime);
            } else {
                log.warn("Could not extract expiration time from refresh token, not blacklisting");
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist old refresh token: {}", e.getMessage());
        }
        
        log.info("Refresh token renewed for user: {}", user.getUsername());
        
        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user
        );
    }
    
    @Override
    public void logout(String refreshToken) {
        // Check if token is valid before blacklisting
        if (!jwtService.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        try {
            // Get token expiration time and blacklist it
            Instant expirationInstant = jwtService.getExpirationTimeFromToken(refreshToken);
            if (expirationInstant != null) {
                long expirationTime = expirationInstant.toEpochMilli();
                tokenBlacklistService.blacklistToken(refreshToken, expirationTime);
                
                String userId = jwtService.getUserIdFromToken(refreshToken);
                log.info("User {} logged out successfully", userId);
            } else {
                log.warn("Could not extract expiration time from refresh token during logout");
                throw new BadCredentialsException("Invalid refresh token");
            }
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            throw new RuntimeException("Logout failed", e);
        }
    }
}
