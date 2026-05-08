package com.salob.food_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;

/**
 * Redis Caching Configuration.
 *
 * Configures Spring Cache abstraction to use Redis with JSON serialization.
 * This allows us to cache Java objects (records, lists) by serializing them to JSON.
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    /**
     * Configure RedisCacheManager with proper JSON serialization.
     *
     * Why this approach?
     * - Uses custom JacksonRedisSerializer (avoids deprecated classes)
     * - ObjectMapper with polymorphic type handling for generic types (List<EateryMapDto>)
     * - Serializes/deserializes cleanly WITH type info embedded in JSON
     * - Handles cache hits and misses correctly
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create ObjectMapper with polymorphic type handling
        var objectMapper = new ObjectMapper();

        // Enable polymorphic type support - Jackson will include @class field in JSON
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        log.info("=== Cache Configuration ===");
        log.info("ObjectMapper configured with polymorphic type support");

        var serializer = new JacksonRedisSerializer(objectMapper);
        log.info("Using custom JacksonRedisSerializer");

        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                )
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        log.info("Cache TTL: 10 minutes");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}






