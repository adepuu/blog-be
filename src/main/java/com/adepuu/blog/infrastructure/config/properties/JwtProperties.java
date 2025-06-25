package com.adepuu.blog.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private String secret = "mySecretKey1234567890123456789012345678901234567890abcdefghijklmnopqrstuvwxyz";
    private String refreshSecret = "myRefreshSecretKey1234567890123456789012345678901234567890abcdefghijklmnopqrstuvwxyz";
    private long accessTokenExpiration = 3600000; // 1 hour in milliseconds
    private long refreshTokenExpiration = 604800000; // 7 days in milliseconds
}
