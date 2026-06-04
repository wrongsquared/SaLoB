package com.salob.food_service.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class IdempotencyManager {
	private static final String IDEMPOTENCY_KEY_PREFIX = "Idempotency:";
	private final StringRedisTemplate redisTemplate;

	public boolean hasKey(String idempotencyKey) {
		String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
		return redisTemplate.hasKey(redisKey);
	}

	public void setKey(String idempotencyKey, Duration ttl) {
		String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
		redisTemplate.opsForValue().setIfAbsent(redisKey, "", ttl);
	}

	public void releaseKey(String idempotencyKey) {
		String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
		redisTemplate.delete(redisKey);
	}
}
