package com.salob.food_service.api.food_entry;

import com.salob.food_service.api._domain.FoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FoodEntryRepository extends JpaRepository<FoodEntry, UUID> {
    List<FoodEntry> findByFood_IdAndEatery_Id(UUID foodId, UUID eateryId);
    List<FoodEntry> findByEatery_IdAndCreatedAtBetween(UUID eateryId, Instant start, Instant end);
    long countBySubmitterId(UUID submitterId);

    /**
     * Fetch food entries whose parent eatery falls within a bounding box.
     * Returns raw rows for service-layer deduplication by confidence.
     */
    @Query(value = """
        SELECT
          fe.id,
          f.label,
          fe.sg_cents,
          e.id,
          e.name,
          ST_Y(e.location) as latitude,
          ST_X(e.location) as longitude
        FROM food_entries fe
        JOIN eateries e ON fe.eatery_id = e.id
        JOIN foods f ON fe.food_id = f.id
        WHERE ST_Contains(
          ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326),
          e.location
        )
        AND e.is_open = TRUE
        ORDER BY e.name, f.label
        """, nativeQuery = true
    )
    List<Object[]> findWithinBoundsWithEateryLocation(
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLon") double minLon,
        @Param("maxLon") double maxLon
    );
}
