package com.salob.food_service.api._helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter using Redis Token Bucket algorithm.
 *
 * What is Token Bucket?
 * =======================
 * Each IP address gets X tokens per minute
 * - Each request costs 1 token
 * - Tokens refill every minute
 * - If no tokens left → request rejected
 *
 * Example: 60 requests per minute per IP
 * - Request 1 at 0:00 → 59 tokens left
 * - Request 2 at 0:00 → 58 tokens left
 * - ...
 * - Request 60 at 0:00 → 0 tokens left
 * - Request 61 at 0:00 → REJECTED (rate limited)
 * - At 1:00 → tokens refilled to 60
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    // Configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_MINUTES = 1L;

    /**
     * Check if request from IP is allowed.
     *
     * @param clientIp client's IP address (from request)
     * @return true if allowed, false if rate limited
     */
    public boolean isRequestAllowed(String clientIp) {
        String key = "ratelimit:bbox:" + clientIp;

        // Get current token count
        String countStr = redisTemplate.opsForValue().get(key);
        long currentCount = countStr == null ? 0 : Long.parseLong(countStr);

        if (currentCount >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return false;  // Reject
        }

        // Increment counter
        redisTemplate.opsForValue().increment(key);

        // Set TTL on first request
        if (currentCount == 0) {
            redisTemplate.expire(key, WINDOW_MINUTES, TimeUnit.MINUTES);
        }

        return true;  // Allow
    }
}
