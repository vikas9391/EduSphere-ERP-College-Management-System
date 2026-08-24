package com.collegeerp.Backend.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Production-safe Redis cache configuration.
 *
 * Redis remains optional for local development: host/port/password have
 * localhost/6379/empty defaults. Cache entries use a short configurable TTL
 * and a dedicated key prefix to avoid collisions with other applications.
 */
@Configuration
@EnableCaching
public class RedisConfiguration {

    @Value("${REDIS_HOST:localhost}")
    private String host;

    @Value("${REDIS_PORT:6379}")
    private int port;

    @Value("${REDIS_PASSWORD:}")
    private String password;

    @Value("${REDIS_CACHE_TTL_SECONDS:600}")
    private long ttlSeconds;

    @Value("${REDIS_KEY_PREFIX:college-erp:}")
    private String keyPrefix;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(host, port);

        if (password != null && !password.isBlank()) {
            configuration.setPassword(password);
        }

        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Duration ttl = Duration.ofSeconds(Math.max(ttlSeconds, 1));

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> keyPrefix + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }
}
