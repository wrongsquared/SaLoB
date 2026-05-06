package com.salob.food_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Custom Redis serializer using Jackson with polymorphic type support.
 *
 * Replaces deprecated Jackson2JsonRedisSerializer.
 * Handles serialization/deserialization of generic types like List<EateryMapDto>.
 */
@Slf4j
public class JacksonRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;
    private static final byte[] EMPTY_ARRAY = new byte[0];

    public JacksonRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("[JacksonRedisSerializer] Initialized with ObjectMapper: {}", objectMapper.getClass().getSimpleName());
    }

    @Override
    public byte[] serialize(Object source) {
        if (source == null) {
            log.debug("[JacksonRedisSerializer.serialize] Source is null, returning empty array");
            return EMPTY_ARRAY;
        }

        try {
            log.debug("[JacksonRedisSerializer.serialize] Serializing object of type: {}", source.getClass().getName());
            byte[] result = objectMapper.writeValueAsBytes(source);
            log.debug("[JacksonRedisSerializer.serialize] Successfully serialized to {} bytes", result.length);
            return result;
        } catch (Exception e) {
            log.error("[JacksonRedisSerializer.serialize] FAILED to serialize object of type: {}", source.getClass().getName(), e);
            throw new RuntimeException("Failed to serialize object of type: " + source.getClass().getName(), e);
        }
    }

    @Override
    public Object deserialize(byte[] source) {
        if (source == null || source.length == 0) {
            log.debug("[JacksonRedisSerializer.deserialize] Source is empty");
            return null;
        }

        try {
            log.debug("[JacksonRedisSerializer.deserialize] Deserializing {} bytes", source.length);
            Object result = objectMapper.readValue(source, Object.class);
            log.debug("[JacksonRedisSerializer.deserialize] Successfully deserialized to type: {}", result.getClass().getName());
            return result;
        } catch (Exception e) {
            log.error("[JacksonRedisSerializer.deserialize] FAILED to deserialize", e);
            throw new RuntimeException("Failed to deserialize object", e);
        }
    }
}


