package com.collegeerp.Backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction backed by the Redis cache manager.
 * Cache entries are configured through application.properties.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {
}
