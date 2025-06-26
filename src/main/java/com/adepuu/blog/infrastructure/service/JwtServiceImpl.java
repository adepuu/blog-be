package com.adepuu.blog.infrastructure.service;

import com.adepuu.blog.domain.entity.User;
import com.adepuu.blog.domain.service.JwtService;
import com.adepuu.blog.infrastructure.config.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
    private final JwsHeader header = JwsHeader.with(() -> "HS256").build();
    private final JwtEncoder accessTokenEncoder;
    private final JwtEncoder refreshTokenEncoder;
    private final JwtDecoder accessTokenDecoder;
    private final JwtDecoder refreshTokenDecoder;
    private final JwtProperties jwtProperties;
    
    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenExpiration(), ChronoUnit.MILLIS);
        
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("blog-app")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", "access")
                .claim("nonce", System.nanoTime()) // Add nanoTime to ensure uniqueness
                .build();
        return accessTokenEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
    
    @Override
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getRefreshTokenExpiration(), ChronoUnit.MILLIS);
        
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("blog-app")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("type", "refresh")
                .claim("nonce", System.nanoTime()) // Add nanoTime to ensure uniqueness
                .build();

        return refreshTokenEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            Jwt jwt = decodeToken(token);
            return !isTokenExpired(jwt);
        } catch (JwtException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getUserIdFromToken(String token) {
        try {
            Jwt jwt = decodeToken(token);
            return jwt.getSubject();
        } catch (JwtException e) {
            log.debug("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getUsernameFromToken(String token) {
        try {
            Jwt jwt = decodeToken(token);
            return jwt.getClaimAsString("username");
        } catch (JwtException e) {
            log.debug("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getRoleFromToken(String token) {
        try {
            Jwt jwt = decodeToken(token);
            return jwt.getClaimAsString("role");
        } catch (JwtException e) {
            log.debug("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public boolean isTokenExpired(String token) {
        try {
            Jwt jwt = decodeToken(token);
            return isTokenExpired(jwt);
        } catch (JwtException e) {
            return true;
        }
    }
    
    @Override
    public Instant getExpirationTimeFromToken(String token) {
        try {
            Jwt jwt = decodeToken(token);
            return jwt.getExpiresAt();
        } catch (JwtException e) {
            log.debug("Failed to extract expiration time from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Decode a token using the appropriate decoder based on token type
     */
    private Jwt decodeToken(String token) {
        // First try to decode as access token
        try {
            Jwt jwt = accessTokenDecoder.decode(token);
            String tokenType = jwt.getClaimAsString("type");
            if ("access".equals(tokenType)) {
                return jwt;
            }
        } catch (JwtException e) {
            // Ignore and try refresh token decoder
        }
        
        // Try to decode as refresh token
        try {
            Jwt jwt = refreshTokenDecoder.decode(token);
            String tokenType = jwt.getClaimAsString("type");
            if ("refresh".equals(tokenType)) {
                return jwt;
            }
        } catch (JwtException e) {
            // Ignore
        }
        
        // If both fail, throw exception
        throw new JwtException("Token could not be decoded as either access or refresh token");
    }
    
    private boolean isTokenExpired(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
