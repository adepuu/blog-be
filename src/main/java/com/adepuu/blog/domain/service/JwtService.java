package com.adepuu.blog.domain.service;

import com.adepuu.blog.domain.entity.User;

import java.time.Instant;

public interface JwtService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    boolean validateToken(String token);
    String getUserIdFromToken(String token);
    String getUsernameFromToken(String token);
    String getRoleFromToken(String token);
    boolean isTokenExpired(String token);
    Instant getExpirationTimeFromToken(String token);
}
