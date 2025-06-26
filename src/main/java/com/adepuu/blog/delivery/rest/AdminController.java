package com.adepuu.blog.delivery.rest;

import com.adepuu.blog.infrastructure.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final TokenBlacklistService tokenBlacklistService;
    
    @GetMapping("/token-blacklist/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getTokenBlacklistStatus() {
        long count = tokenBlacklistService.getBlacklistedTokenCount();
        boolean healthy = tokenBlacklistService.isHealthy();
        
        return Map.of(
            "blacklistedTokenCount", count,
            "isHealthy", healthy,
            "service", "redis-backed"
        );
    }
    
    @GetMapping("/token-blacklist/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> clearTokenBlacklist() {
        long beforeCount = tokenBlacklistService.getBlacklistedTokenCount();
        tokenBlacklistService.clearAll();
        long afterCount = tokenBlacklistService.getBlacklistedTokenCount();
        
        log.warn("Admin cleared token blacklist. Before: {}, After: {}", beforeCount, afterCount);
        
        return Map.of(
            "message", "Token blacklist cleared",
            "tokensCleared", beforeCount,
            "remainingTokens", afterCount
        );
    }
}
