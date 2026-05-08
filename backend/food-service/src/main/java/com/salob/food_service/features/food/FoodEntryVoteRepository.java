package com.salob.food_service.features.food;

import com.salob.food_service.features.food.domain.FoodEntryVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FoodEntryVoteRepository extends JpaRepository<FoodEntryVote, UUID> {
    /**
     * Check if a vote already exists for this voter and food entry.
     * Used to enforce the unique constraint (voter_id, food_entry_id) idempotently.
     */
    boolean existsByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);
}
