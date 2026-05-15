package com.salob.food_service.api._helpers;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

/**
 * Type-safe cache key generator for bounding box queries.
 *
 * WHY THIS IS BETTER:
 * ====================
 * Instead of:
 *   @Cacheable(key = "@bboxCacheKeyGenerator.generate(#minLat, ...)")
 *   └─ Fragile: string-based, breaks if method names change, no IDE support
 *
 * We use Spring's KeyGenerator interface:
 *   @Cacheable(keyGenerator = "bboxKeyGenerator")
 *   └─ Type-safe: Java method references, IDE support, refactor-friendly
 *
 * This is the "official" Spring way to do custom key generation.
 */
@Component(value = "bboxKeyGenerator")
public class BboxKeyGenerator implements KeyGenerator {

    /**
     * Generate a cache key based on method parameters.
     *
     * Spring calls this automatically when @Cacheable is used with this KeyGenerator.
     *
     * @param target  the object instance (we don't use it here)
     * @param method  the method being called (we don't use it here)
     * @param params  the method parameters: [minLat, maxLat, minLon, maxLon]
     * @return cache key string
     */
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length != 4) {
            throw new IllegalArgumentException(
                "BboxKeyGenerator expects exactly 4 parameters (minLat, maxLat, minLon, maxLon), got " + params.length
            );
        }

        double minLat = (double) params[0];
        double maxLat = (double) params[1];
        double minLon = (double) params[2];
        double maxLon = (double) params[3];

        return generateBucketedKey(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Generate bucketed cache key (round coordinates to 0.01° grid).
     *
     * @param minLat southern boundary
     * @param maxLat northern boundary
     * @param minLon western boundary
     * @param maxLon eastern boundary
     * @return cache key like "bbox:1.27:1.32:103.80:103.86"
     */
    private String generateBucketedKey(double minLat, double maxLat, double minLon, double maxLon) {
        // Round to 2 decimal places (0.01° ≈ 1.1 km)
        double bucketMinLat = Math.floor(minLat * 100) / 100.0;
        double bucketMaxLat = Math.floor(maxLat * 100) / 100.0;
        double bucketMinLon = Math.floor(minLon * 100) / 100.0;
        double bucketMaxLon = Math.floor(maxLon * 100) / 100.0;

        return String.format("bbox:%.2f:%.2f:%.2f:%.2f",
                bucketMinLat, bucketMaxLat, bucketMinLon, bucketMaxLon);
    }
}

