package com.adepuu.blog.domain.service;

import com.adepuu.blog.delivery.dto.auth.AuthResponse;
import com.adepuu.blog.delivery.dto.auth.LoginRequest;
import com.adepuu.blog.delivery.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
}
