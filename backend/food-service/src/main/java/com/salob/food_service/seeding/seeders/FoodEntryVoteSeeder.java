package com.salob.food_service.seeding.seeders;

import com.salob.food_service.api.food_entry_vote.FoodEntryVoteRepository;
import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api._domain.FoodEntryVote;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds random food entry votes for testing.
 *
 * Unique constraint: (voter_id, food_entry_id) - one vote per voter per entry.
 * This seeder handles duplicates gracefully (idempotent): - If a vote already
 * exists, it's skipped - Safe to run multiple times without errors
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FoodEntryVoteSeeder {
	private static final int MAX_VOTES_PER_ENTRY = 80;
	private final FoodEntryVoteRepository foodEntryVoteRepository;

	/**
	 * Seed votes for food entries.
	 *
	 * @param userIDs
	 *            list of user IDs to randomly draw voters from
	 * @param foodEntries
	 *            list of food entries to vote on
	 * @return number of votes created (skips duplicates)
	 */
	@Transactional
	public int seed(List<UUID> userIDs, List<FoodEntry> foodEntries) {
		if (userIDs.isEmpty() || foodEntries.isEmpty()) {
			log.warn("Skipping vote seeding: missing users or food entries");
			return 0;
		}

		ThreadLocalRandom random = ThreadLocalRandom.current();
		List<FoodEntryVote> votes = new ArrayList<>();
		Set<String> usedPairs = new HashSet<>();
		Instant now = Instant.now();
		long daysInSeconds = 365L * 24 * 60 * 60;

		for (FoodEntry foodEntry : foodEntries) {
			int maxVotesForThisEntry = Math.min(userIDs.size(), MAX_VOTES_PER_ENTRY);
			int numVotes = random.nextInt(1, maxVotesForThisEntry + 1);

			List<UUID> shuffledUsers = new ArrayList<>(userIDs);
			Collections.shuffle(shuffledUsers, random);

			for (int i = 0; i < numVotes; i++) {
				UUID voterId = shuffledUsers.get(i);
				String pairKey = voterId + ":" + foodEntry.getId();

				if (usedPairs.contains(pairKey)) {
					continue;
				}

				if (foodEntryVoteRepository.existsByVoterIdAndFoodEntryId(voterId, foodEntry.getId())) {
					usedPairs.add(pairKey);
					continue;
				}

				boolean isUpvote = random.nextBoolean();
				FoodEntryVote vote = FoodEntryVote.builder().foodEntry(foodEntry).voterId(voterId).isUpvote(isUpvote)
						.build();
				vote.setCreatedAt(now.minusSeconds((long) (random.nextDouble() * daysInSeconds)));

				votes.add(vote);
				usedPairs.add(pairKey);
			}
		}

		foodEntryVoteRepository.saveAll(votes);
		log.info("Seeded {} food entry votes", votes.size());
		return votes.size();
	}
}
