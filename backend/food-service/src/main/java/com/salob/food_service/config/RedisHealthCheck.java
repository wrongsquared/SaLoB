package com.salob.food_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Validates Redis connectivity at application startup with detailed diagnostics.
 *
 * Why this matters:
 * - Redis connection pool doesn't fail fast by default
 * - App starts fine even if Redis is down (lazy initialization)
 * - First actual cache operation fails with unclear error
 * - This component ensures we know immediately if Redis is unavailable
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthCheck {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisConnectionFactory connectionFactory;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout}")
    private String redisTimeout;

    @EventListener(ApplicationReadyEvent.class)
    public void checkRedisConnectivity() {
        try {
            log.info("=== Redis Connection Verification ===");

            // Log connection details
            String maskedPassword = redisPassword != null && !redisPassword.isEmpty()
                ? "***" + redisPassword.substring(Math.max(0, redisPassword.length() - 3))
                : "(none)";
            String connectionUrl = String.format("redis://%s:%d", redisHost, redisPort);

            log.info("Connection URL: {}", connectionUrl);
            log.info("Host: {}", redisHost);
            log.info("Port: {}", redisPort);
            log.info("Password: {}", maskedPassword);
            log.info("Timeout: {}", redisTimeout);
            log.info("ConnectionFactory type: {}", connectionFactory.getClass().getSimpleName());
            log.info("");  // blank line for readability

            // Test SET operation
            String testKey = "redis_health_check_" + System.currentTimeMillis();
            String testValue = "healthy_at_" + System.currentTimeMillis();

            log.info("Testing SET operation on key: {}", testKey);
            redisTemplate.opsForValue().set(testKey, testValue, Duration.ofMinutes(5));
            log.info("✓ SET operation successful");

            // Retrieve value
            String retrieved = redisTemplate.opsForValue().get(testKey);
            log.info("✓ GET operation successful");
            log.info("  Stored: {}", testValue);
            log.info("  Retrieved: {}", retrieved);

            // Check TTL
            Long ttl = redisTemplate.getExpire(testKey, TimeUnit.SECONDS);
            log.info("  Key TTL: {} seconds", ttl);

            if (testValue.equals(retrieved)) {
                log.info("✓✓✓ Redis connection verified successfully - cache is operational ✓✓✓");
            } else {
                log.warn("⚠ Redis returned unexpected value from health check");
                log.warn("  Expected: {}", testValue);
                log.warn("  Got: {}", retrieved);
            }

        } catch (Exception e) {
            log.error("✗✗✗ CRITICAL: Redis is not available! Caching will not work. ✗✗✗", e);
            log.error("Error: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("Verify Redis is running and accessible");
            log.error("Expected connection: redis://{}:{}", redisHost, redisPort);
        }
    }
}








