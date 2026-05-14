package com.salob.food_service.features.food;

import com.salob.food_service.features.food.domain.FoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodEntryRepository extends JpaRepository<FoodEntry, UUID> {
    List<FoodEntry> findByFood_IdAndEatery_Id(UUID foodId, UUID eateryId);

    List<FoodEntry> findByEatery_IdAndCreatedAtBetween(UUID eateryId, Instant start, Instant end);

    long countBySubmitterId(UUID submitterId);

    Optional<FoodEntry> findFirstBySubmitterIdOrderByCreatedAtAsc(UUID submitterId);
}
