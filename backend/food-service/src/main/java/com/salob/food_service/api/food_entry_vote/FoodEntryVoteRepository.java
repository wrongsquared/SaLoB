package com.salob.food_service.api.food_entry_vote;

import com.salob.food_service.api._domain.FoodEntryVote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.salob.food_service.api.food_entry_vote.projections.FoodEntryVoteProjection;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodEntryVoteRepository extends JpaRepository<FoodEntryVote, UUID> {
	boolean existsByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);
	Optional<FoodEntryVote> findByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);
	void deleteById(@NonNull UUID id);

	@Query(value = """
				SELECT e.submitter_id AS foodEntrySubmitterId, v.is_upvote AS isUpvote
				FROM food_entries e
				LEFT JOIN food_entry_votes v
			    ON v.food_entry_id = e.id AND v.voter_id = :voterId
				WHERE e.id = :foodEntryId
			""", nativeQuery = true)
	Optional<FoodEntryVoteProjection> findExistingVoteProjection(@Param("voterId") UUID voterId,
			@Param("foodEntryId") UUID foodEntryId);

	@Modifying
	@Query(value = """
				INSERT INTO food_entry_votes (voter_id, food_entry_id, is_upvote, created_at)
				VALUES (:voterId, :foodEntryId, :isUpvote, NOW())
				ON CONFLICT (voter_id, food_entry_id)
				DO UPDATE SET is_upvote = EXCLUDED.is_upvote
				WHERE food_entry_votes.is_upvote != EXCLUDED.is_upvote
			""", nativeQuery = true)
	int upsertVote(@Param("voterId") UUID voterId, @Param("foodEntryId") UUID foodEntryId,
			@Param("isUpvote") boolean isUpvote);

	@Modifying
	@Query(value = """
				DELETE FROM food_entry_votes
				WHERE voter_id = :voterId AND food_entry_id = :foodEntryId
			""", nativeQuery = true)
	int deleteVote(@Param("voterId") UUID voterId, @Param("foodEntryId") UUID foodEntryId);

	@Query("SELECT v FROM FoodEntryVote v WHERE v.foodEntry.id IN :foodEntryIds AND v.voterId = :voterId")
	List<FoodEntryVote> findByVoterIdAndFoodEntryIdIn(@Param("voterId") UUID voterId,
			@Param("foodEntryIds") List<UUID> foodEntryIds);
}
