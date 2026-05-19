package com.salob.food_service.api.food_entry;

import com.salob.food_service.api.food_entry.dto.FoodEntrySubmissionRequest;
import com.salob.food_service.common.Utils;
import com.salob.food_service.api.food_entry.dto.FoodEntryDetailedDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryHistoricalDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryMapDTO;
import com.salob.food_service.api._helpers.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/food-entries")
public class FoodEntryController {
    private final FoodEntryService foodEntryService;
    private final RateLimiter rateLimiter;

    @GetMapping("/historical-data/{foodEntryId}")
    public ResponseEntity<FoodEntryHistoricalDTO> getFoodEntryHistoricalData(
            @Valid @PathVariable UUID foodEntryId,
            @Valid @RequestParam Instant startDate,
            HttpServletRequest request
    ) {
        String clientIP = Utils.getClientIp(request);
        if (!rateLimiter.isRequestAllowed(clientIP)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // startDate cannot be in the future
        if (startDate.isAfter(Instant.now())) {
            return ResponseEntity.badRequest().build();
        }

        // 1-year hard limit - clamp
        Instant oneYearAgo = Instant.now().minus(Duration.ofDays(365));
        Instant clampedStartDate = startDate.isBefore(oneYearAgo) ? oneYearAgo : startDate;

        FoodEntryHistoricalDTO foodEntryDetailed = foodEntryService.getFoodEntryHistoricalData(foodEntryId, clampedStartDate);
        return ResponseEntity.ok(foodEntryDetailed);
    }

    @GetMapping("/{foodEntryId}/details")
    public ResponseEntity<FoodEntryDetailedDTO> getFoodEntryDetails(
            @Valid @PathVariable UUID foodEntryId,
            HttpServletRequest request
    ) {
        String clientIP = Utils.getClientIp(request);
        if (!rateLimiter.isRequestAllowed(clientIP)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        FoodEntryDetailedDTO details = foodEntryService.getFoodEntryDetailed(foodEntryId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/submit")
    public ResponseEntity<Void> submitFoodEntry(
            @Valid @RequestHeader("X-User-Id") UUID id,
            @Valid@RequestBody FoodEntrySubmissionRequest req
    ) {
        foodEntryService.submitFoodEntry(id, req);
        return ResponseEntity.ok().build();
    }

    /**
     * Fetch food entries within a bounding box (food mode map view).
     * Results are deduplicated by (eatery, food), keeping highest confidence.
     * Example: /api/food-entries/within-bounds?minLat=1.27&maxLat=1.32&minLon=103.80&maxLon=103.86
     */
    @GetMapping("/within-bounds")
    public ResponseEntity<List<FoodEntryMapDTO>> getFoodEntriesWithinBounds(
            @Valid @RequestParam double minLat,
            @Valid @RequestParam double maxLat,
            @Valid @RequestParam double minLon,
            @Valid @RequestParam double maxLon,
            HttpServletRequest request
    ) {
        String clientIp = Utils.getClientIp(request);
        if (!rateLimiter.isRequestAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        if (minLat >= maxLat || minLon >= maxLon) {
            return ResponseEntity.badRequest().build();
        }

        List<FoodEntryMapDTO> entries = foodEntryService.findFoodEntriesWithinBounds(minLat, maxLat, minLon, maxLon);
        return ResponseEntity.ok(entries);
    }
}
