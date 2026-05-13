package com.salob.food_service.features.eatery;

import com.salob.food_service.common.Utils;
import com.salob.food_service.features.eatery.dto.EateryDetailedDTO;
import com.salob.food_service.features.eatery.dto.EateryPreviewDTO;
import com.salob.food_service.features.eatery.helpers.RateLimiter;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST API controller for eatery-related endpoints.
 *
 * This controller handles HTTP requests for eatery data, particularly
 * geospatial queries for map interfaces.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/eateries")
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
    public ResponseEntity<List<EateryPreviewDTO>> getEateriesWithinBounds(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLon,
            @RequestParam double maxLon,
            HttpServletRequest request
    ) {
        String clientIp = Utils.getClientIp(request);
        if (!rateLimiter.isRequestAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        if (minLat >= maxLat || minLon >= maxLon) {
            return ResponseEntity.badRequest().build();
        }

        List<EateryPreviewDTO> eateries = eateryService.findEateriesWithinBounds(minLat, maxLat, minLon, maxLon);
        return ResponseEntity.ok(eateries);
    }

    /**
     * Example: /api/eateries/fg233ae7-a797-48b8-b6ee-c0ecc4a983e8
     */
    @GetMapping("/{eateryId}")
    public ResponseEntity<EateryDetailedDTO> getEateryDetailed(@PathVariable UUID eateryId, HttpServletRequest request) {
        String clientIP = Utils.getClientIp(request);
        if (!rateLimiter.isRequestAllowed(clientIP)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.ok(eateryService.getEateryDetailed(eateryId));
    }

}

