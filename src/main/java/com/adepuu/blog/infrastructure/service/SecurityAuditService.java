package com.adepuu.blog.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SecurityAuditService {
    
    private final Map<String, Integer> failedLoginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    
    /**
     * Log security events for monitoring and compliance
     */
    public void logSecurityEvent(String event, String userId, String details) {
        log.info("SECURITY_EVENT: {} | User: {} | Details: {} | Timestamp: {}", 
            event, userId, details, OffsetDateTime.now());
    }
    
    /**
     * Log successful login
     */
    public void logSuccessfulLogin(String userId, String ipAddress, String userAgent) {
        // Reset failed attempts on successful login
        failedLoginAttempts.remove(userId);
        
        logSecurityEvent("LOGIN_SUCCESS", userId, 
            String.format("IP: %s, UserAgent: %s", ipAddress, userAgent));
    }
    
    /**
     * Log failed login attempt
     */
    public void logFailedLogin(String identifier, String ipAddress, String userAgent) {
        // Increment failed attempts
        failedLoginAttempts.merge(identifier, 1, Integer::sum);
        
        int attempts = failedLoginAttempts.get(identifier);
        
        logSecurityEvent("LOGIN_FAILED", identifier, 
            String.format("IP: %s, UserAgent: %s, Attempts: %d", ipAddress, userAgent, attempts));
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            logSecurityEvent("ACCOUNT_LOCKED", identifier, 
                String.format("Too many failed attempts from IP: %s", ipAddress));
        }
    }
    
    /**
     * Log user registration
     */
    public void logUserRegistration(String userId, String ipAddress) {
        logSecurityEvent("USER_REGISTRATION", userId, String.format("IP: %s", ipAddress));
    }
    
    /**
     * Log password changes
     */
    public void logPasswordChange(String userId, String ipAddress) {
        logSecurityEvent("PASSWORD_CHANGE", userId, String.format("IP: %s", ipAddress));
    }
    
    /**
     * Log privilege escalation attempts
     */
    public void logPrivilegeEscalation(String userId, String action, String ipAddress) {
        logSecurityEvent("PRIVILEGE_ESCALATION", userId, 
            String.format("Action: %s, IP: %s", action, ipAddress));
    }
    
    /**
     * Log content moderation events
     */
    public void logContentModeration(String moderatorId, String action, String targetId, String reason) {
        logSecurityEvent("CONTENT_MODERATION", moderatorId, 
            String.format("Action: %s, Target: %s, Reason: %s", action, targetId, reason));
    }
    
    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String userId, String activity, String ipAddress) {
        logSecurityEvent("SUSPICIOUS_ACTIVITY", userId, 
            String.format("Activity: %s, IP: %s", activity, ipAddress));
    }
    
    /**
     * Log API rate limiting events
     */
    public void logRateLimitExceeded(String userId, String operation, String ipAddress) {
        logSecurityEvent("RATE_LIMIT_EXCEEDED", userId, 
            String.format("Operation: %s, IP: %s", operation, ipAddress));
    }
    
    /**
     * Log data access events for sensitive operations
     */
    public void logDataAccess(String userId, String resource, String operation) {
        logSecurityEvent("DATA_ACCESS", userId, 
            String.format("Resource: %s, Operation: %s", resource, operation));
    }
    
    /**
     * Check if account should be locked due to failed attempts
     */
    public boolean shouldLockAccount(String identifier) {
        return failedLoginAttempts.getOrDefault(identifier, 0) >= MAX_FAILED_ATTEMPTS;
    }
    
    /**
     * Reset failed login attempts for an identifier
     */
    public void resetFailedAttempts(String identifier) {
        failedLoginAttempts.remove(identifier);
        logSecurityEvent("FAILED_ATTEMPTS_RESET", identifier, "Manual reset");
    }
    
    /**
     * Get current failed attempts count
     */
    public int getFailedAttemptsCount(String identifier) {
        return failedLoginAttempts.getOrDefault(identifier, 0);
    }
}
