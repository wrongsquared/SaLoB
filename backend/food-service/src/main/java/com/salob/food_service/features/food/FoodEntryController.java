package com.salob.food_service.features.food;

import com.salob.food_service.common.Utils;
import com.salob.food_service.features.eatery.dto.FoodDetailedDTO;
import com.salob.food_service.features.eatery.helpers.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/food-entries")
public class FoodEntryController {
    private final FoodEntryService foodEntryService;
    private final RateLimiter rateLimiter;

    @GetMapping("/{foodEntryId}")
    public ResponseEntity<FoodDetailedDTO> getFoodEntryDetailed(
            @PathVariable UUID foodEntryId,
            @RequestParam(required = true) Instant startDate,
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
        return ResponseEntity.ok(foodEntryService.getFoodEntryDetailed(foodEntryId, clampedStartDate));
    }
}
