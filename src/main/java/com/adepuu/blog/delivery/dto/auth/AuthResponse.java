package com.adepuu.blog.delivery.dto.auth;

import com.adepuu.blog.domain.entity.User;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        User user) {
};
