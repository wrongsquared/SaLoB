package com.salob.food_service.api.eatery;

import java.util.List;
import java.util.UUID;

import com.salob.food_service.api._domain.Eatery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EateryRepository extends JpaRepository<Eatery, UUID> {
    boolean existsByName(String name);

    @Query(value = """
        SELECT e.id, e.name, e.address
        FROM eateries e
        WHERE e.name ILIKE %:search% OR e.address ILIKE %:search%
        LIMIT 20 -- Hardcoded limit
        """, nativeQuery = true)
    List<Object[]> findBySearchCaseInsensitive(String search);

    /**
     * Find all eateries within a geographic bounding box.
     *
     * This uses a native SQL query with PostGIS functions:
     * - ST_MakeEnvelope: Creates a rectangle from (minLon, minLat) to (maxLon, maxLat)
     * - ST_Contains: Checks if the eatery's point location is inside the rectangle
     * - SRID 4326: WGS84 coordinate system (standard GPS)
     *
     * Why native query? JPQL doesn't have good geometry support, so we use raw SQL
     * with PostGIS functions that the database engine optimizes with spatial indices.
     *
     * @param minLat southern boundary (example: 1.27)
     * @param maxLat northern boundary (example: 1.32)
     * @param minLon western boundary (example: 103.80)
     * @param maxLon eastern boundary (example: 103.86)
     * @return lightweight DTOs suitable for map rendering
     */
    @Query(value = """
        SELECT
          e.id,
          e.name,
          ST_Y(e.location) as latitude,
          ST_X(e.location) as longitude,
          et.label as typeLabel
        FROM eateries e
        JOIN eatery_types et ON e.type_id = et.id
        WHERE ST_Contains(
          ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326),
          e.location
        )
        AND e.is_open = TRUE
        ORDER BY e.name
        """, nativeQuery = true
    )
    List<Object[]> findWithinBoundingBox(
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLon") double minLon,
        @Param("maxLon") double maxLon
    );
}
