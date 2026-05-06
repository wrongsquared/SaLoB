package com.salob.food_service.api.eatery;

import com.salob.food_service.api.eatery.dto.EateryMapDto;
import com.salob.food_service.api.eatery.services.EateryService;
import com.salob.food_service.api.eatery.services.RateLimiter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST API controller for eatery-related endpoints.
 *
 * This controller handles HTTP requests for eatery data, particularly
 * geospatial queries for map interfaces.
 */
@RestController
@RequestMapping("/api/eateries")
@RequiredArgsConstructor
public class EateryController {

    private final EateryService eateryService;
    private final RateLimiter rateLimiter;

    /**
     * Fetch eateries within a bounding box (map view).
     *
     * This endpoint is designed for map interfaces that need eateries within a
     * geographical region. Results are cached aggressively using coordinate bucketing
     * to improve cache hit rates during continuous panning.
     *
     * @param minLat minimum latitude (southern boundary)
     * @param maxLat maximum latitude (northern boundary)
     * @param minLon minimum longitude (western boundary)
     * @param maxLon maximum longitude (eastern boundary)
     * @return list of eateries within the bounding box, with minimal details (optimized for rendering)
     *
     * Example: /api/eateries/within-bounds?minLat=1.27&maxLat=1.32&minLon=103.80&maxLon=103.86
     */
    @GetMapping("/within-bounds")
    public ResponseEntity<List<EateryMapDto>> getEateriesWithinBounds(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLon,
            @RequestParam double maxLon,
            HttpServletRequest request
    ) {
        // Extract client IP for rate limiting
        String clientIp = getClientIp(request);

        // Check rate limit
        if (!rateLimiter.isRequestAllowed(clientIp)) {
            // 429 Too Many Requests - rate limited
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // Validate bounding box is sensible
        if (minLat >= maxLat || minLon >= maxLon) {
            return ResponseEntity.badRequest().build();
        }

        // Delegate to service layer for business logic (caching, DB query)
        List<EateryMapDto> eateries = eateryService.findEateriesWithinBounds(minLat, maxLat, minLon, maxLon);

        return ResponseEntity.ok(eateries);
    }

    /**
     * Extract client IP from HTTP request.
     *
     * Handles proxies (X-Forwarded-For header) which is important in production
     * where requests may go through load balancers or reverse proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can have multiple IPs; take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

