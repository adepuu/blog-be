package com.adepuu.blog.infrastructure.config;

import com.adepuu.blog.infrastructure.config.properties.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class JwtConfig {

    private final JwtProperties jwtProperties;

    public JwtConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = new SecretKeySpec(jwtProperties.getSecret().getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    @Primary
    public JwtEncoder jwtEncoder() {
        SecretKey secretKey = new SecretKeySpec(jwtProperties.getSecret().getBytes(), "HmacSHA256");
        JWKSource<SecurityContext> immutableSecret = new ImmutableSecret<>(secretKey);
        return new NimbusJwtEncoder(immutableSecret);
    }

    @Bean
    public JwtDecoder refreshTokenDecoder() {
        SecretKey refreshSecretKey = new SecretKeySpec(jwtProperties.getRefreshSecret().getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(refreshSecretKey).build();
    }

    @Bean
    public JwtEncoder refreshTokenEncoder() {
        SecretKey refreshSecretKey = new SecretKeySpec(jwtProperties.getRefreshSecret().getBytes(), "HmacSHA256");
        JWKSource<SecurityContext> immutableRefreshSecret = new ImmutableSecret<>(refreshSecretKey);
        return new NimbusJwtEncoder(immutableRefreshSecret);
    }
}
