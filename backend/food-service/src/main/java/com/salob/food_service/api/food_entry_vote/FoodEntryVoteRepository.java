package com.salob.food_service.api.food_entry_vote;

import com.salob.food_service.api._domain.FoodEntryVote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodEntryVoteRepository extends JpaRepository<FoodEntryVote, UUID> {
	boolean existsByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);

	Optional<FoodEntryVote> findByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);

	void deleteById(UUID id);
	void deleteByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);

	@Query("SELECT v FROM FoodEntryVote v WHERE v.foodEntry.id IN :foodEntryIds AND v.voterId = :voterId")
	List<FoodEntryVote> findByVoterIdAndFoodEntryIdIn(@Param("voterId") UUID voterId,
			@Param("foodEntryIds") List<UUID> foodEntryIds);
}
