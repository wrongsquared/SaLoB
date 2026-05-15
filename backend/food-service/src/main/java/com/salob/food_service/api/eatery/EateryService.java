package com.salob.food_service.api.eatery;

import com.salob.food_service.common.ConfidenceAlgorithm;
import com.salob.food_service.api._domain.Eatery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.salob.food_service.api.eatery.dto.EateryDetailedDTO;
import com.salob.food_service.api.eatery.dto.EateryPreviewDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryPreviewDTO;
import com.salob.food_service.api._exceptions.EateryNotFoundException;
import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.storage.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
/**
 * Service layer for eatery business logic.
 *
 * Services in Spring encapsulate business rules and coordinate multiple components.
 * This service handles:
 * 1. Geospatial queries (delegated to repository)
 * 2. Caching layer (declared with @Cacheable)
 * 3. Data mapping (Eatery entity -> EateryMapDto)
 *
 * The @Service annotation tells Spring to manage this as a singleton bean,
 * making it injectable into controllers and other services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EateryService {
    private final EateryRepository eateryRepo;
    private final ConfidenceAlgorithm confidenceAlgorithm;
    private final MinioStorageService minioStorageService;

    public Eatery findById(UUID id) {
        return eateryRepo.findById(id).orElseThrow(() -> new EateryNotFoundException(id));
    }

    /**
     * Find eateries within a bounding box, with intelligent caching.
     *
     * CACHING STRATEGY (learn why this matters):
     * =====================================================
     * Problem: User pans map continuously. Each pixel drag = slightly different bbox.
     *          Without bucketing, nearly every request = new cache key = DB hit.
     *
     * Solution: COORDINATE BUCKETING
     *   - Round bbox to 0.01° grid (about 1km precision, imperceptible on map)
     *   - All requests to similar areas → same cache key → cache hit
     *   - Trade-off: small precision loss for massive cache improvements
     *
     * Example:
     *   Request 1: bbox=[1.2700, 1.3200, 103.8000, 103.8600] → bucket key=[1.27, 1.32, 103.80, 103.86]
     *   Request 2: bbox=[1.2705, 1.3198, 103.8001, 103.8599] → SAME bucket key (cache hit!)
     *   Request 3: bbox=[1.2800, 1.3300, 103.9000, 103.9600] → different bucket key (cache miss, but new area)
     *
     * @param minLat southern boundary
     * @param maxLat northern boundary
     * @param minLon western boundary
     * @param maxLon eastern boundary
     * @return list of eateries, pulled from cache or DB
     */
    @Cacheable(
        value = "eateries_bbox",
        keyGenerator = "bboxKeyGenerator"
    )
    public List<EateryPreviewDTO> findEateriesWithinBounds(
            double minLat,
            double maxLat,
            double minLon,
            double maxLon
    ) {
        // This log only appears on CACHE MISS (method actually executes)
        log.info("=== CACHE MISS ===");
        log.info("Querying database for bbox=[{}, {}, {}, {}]", minLat, maxLat, minLon, maxLon);

        try {
            // Query database for eateries within bounds
            log.debug("Executing PostGIS query...");
            List<Object[]> rows = eateryRepo.findWithinBoundingBox(minLat, maxLat, minLon, maxLon);
            log.debug("Query returned {} rows", rows.size());

            // IMPORTANT: Use ArrayList (mutable), NOT .toList() (ImmutableCollections$ListN)
            // Jackson can deserialize ArrayList but NOT internal immutable list types
            List<EateryPreviewDTO> result = new ArrayList<>();
            for (Object[] row : rows) {
                result.add(mapRowToDto(row));
            }
            
            log.info("Found {} eateries, about to cache result", result.size());
            log.debug("Result type: {}, Result class: {}", result.getClass().getName(), result.getClass().getSimpleName());
            
            // Note: @Cacheable will now try to serialize this result
            // If serialization fails, an exception will be thrown after this method returns
            return result;

        } catch (Exception e) {
            log.error("ERROR in findEateriesWithinBounds", e);
            throw e;
        }
    }

    /**
     * This endpoint is meant to be used for the collapsible left-panel that expands when you click on an eatery on the map.
     *
     * For some eatery (eateryId), get its detailed information AND list of the "best" food entries for each food it serves.
     * E.g An eatery may have 23 entries for "chicken rice", but you want to show the one with the highest confidence
     */
    public EateryDetailedDTO getEateryDetailed(UUID eateryId) {
        Eatery eatery = findById(eateryId);

        // For each food served by the eatery, find the "best" food entry (the one with the highest confidence score).
        Map<String, FoodEntryPreviewDTO> bestByFoodName = new LinkedHashMap<>();
        Map<String, Double> bestConfidenceByFoodName = new LinkedHashMap<>();
        for (FoodEntry foodEntry : eatery.getFoodEntries()) {
            String foodName = foodEntry.getFood().getLabel();
            double confidence = confidenceAlgorithm.computeFinalConfidence(foodEntry);
            Double currentBest = bestConfidenceByFoodName.get(foodName);
            if (currentBest == null || confidence > currentBest) {
                bestConfidenceByFoodName.put(foodName, confidence);
                bestByFoodName.put(
                    foodName,
                    new FoodEntryPreviewDTO(
                        foodEntry.getId(),
                        foodName,
                        foodEntry.getSgCents(),
                        foodEntry.getUpvoteCount(),
                        foodEntry.getDownvoteCount(),
                        minioStorageService.getPresignedUrl(
                            foodEntry.getFood().getPhotoObjKey(),
                            Duration.ofMinutes(30)
                        ),
                        foodEntry.getSubmitterId(),
                        null,
                        foodEntry.getCreatedAt()
                    )
                );
            }
        }

        List<FoodEntryPreviewDTO> foodPreviews = new ArrayList<>(bestByFoodName.values());
        return new EateryDetailedDTO(
                eatery.getId(),
                eatery.getName(),
                eatery.getAddress(),
                eatery.getType().getLabel(),
                minioStorageService.getPresignedUrl(
                    eatery.getPhotoObjKey(),
                    Duration.ofMinutes(30)
                ),
                foodPreviews
        );
    }

    /**
     * Convert a database query result row into a DTO.
     *
     * The repository returns Object[] because of the native SQL query.
     * We need to manually map columns to the DTO record.
     *
     * @param row raw database result (e.g., [eateryId, foodName, lat, lon, typeLabel, isClosed])
     * @return strongly-typed DTO
     */
    private EateryPreviewDTO mapRowToDto(Object[] row) {
        return new EateryPreviewDTO(
            (UUID) row[0],           // eateryId
            (String) row[1],         // foodName
            ((Number) row[2]).doubleValue(),  // latitude (ST_Y as Double)
            ((Number) row[3]).doubleValue(),  // longitude (ST_X as Double)
            (String) row[4]         // typeLabel
        );
    }
}
