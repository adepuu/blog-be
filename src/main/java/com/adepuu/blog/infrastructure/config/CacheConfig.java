package com.adepuu.blog.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Use simple concurrent map cache manager as fallback
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of("posts", "users", "tags", "comments"));
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
    
    // Caffeine cache configuration - only if Caffeine is available
    @Configuration
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
    static class CaffeineConfig {
        
        @Bean
        public CacheManager caffeineCacheManager() {
            try {
                // Use reflection to avoid compile-time dependency
                Class<?> caffeineCacheManagerClass = Class.forName("org.springframework.cache.caffeine.CaffeineCacheManager");
                Object cacheManager = caffeineCacheManagerClass.getDeclaredConstructor().newInstance();
                
                // Set cache names
                java.lang.reflect.Method setCacheNames = caffeineCacheManagerClass.getMethod("setCacheNames", java.util.Collection.class);
                setCacheNames.invoke(cacheManager, List.of("posts", "users", "tags", "comments"));
                
                return (CacheManager) cacheManager;
            } catch (Exception e) {
                // Fall back to concurrent map cache manager
                ConcurrentMapCacheManager fallback = new ConcurrentMapCacheManager();
                fallback.setCacheNames(List.of("posts", "users", "tags", "comments"));
                return fallback;
            }
        }
    }
}
