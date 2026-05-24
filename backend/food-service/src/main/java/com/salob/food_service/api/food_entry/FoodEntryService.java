package com.salob.food_service.api.food_entry;

import com.salob.food_service.api._domain.Eatery;
import com.salob.food_service.api._domain.Food;
import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api._domain.FoodEntryVote;
import com.salob.food_service.api.eatery.EateryRepository;
import com.salob.food_service.api.food.FoodRepository;
import com.salob.food_service.api.food_entry.dto.DatePrice;
import com.salob.food_service.api.food_entry.dto.FoodEntryDetailedDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryHistoricalDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryMapDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryPreviewDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntrySubmissionRequest;
import com.salob.food_service.api.food_entry_vote.FoodEntryVoteRepository;
import com.salob.food_service.common.ConfidenceAlgorithm;
import com.salob.food_service.common.rabbitmq.WtfEventPublisher;
import com.salob.food_service.storage.minio.MinioStorageService;
import com.salob.proto.events.VoteType;
import com.salob.proto.user.UserDetailsBatchRequest;
import com.salob.proto.user.UserDetailsBatchResponse;
import com.salob.proto.user.UserDetailsBatchResponseItem;
import com.salob.proto.user.UserDetailsRequest;
import com.salob.proto.user.UserDetailsResponse;
import com.salob.proto.user.UserServiceGrpc;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FoodEntryService {

	private final FoodEntryRepository foodEntryRepo;
	private final EateryRepository eateryRepo;
	private final FoodRepository foodRepo;

	private final ConfidenceAlgorithm confidenceAlgo;
	private final MinioStorageService minioService;
	private final WtfEventPublisher wtfEventPublisher;
	private final FoodEntryVoteRepository foodEntryVoteRepo;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	/**
	 * Returns historical pricing data for a food entry: the consensus price, a
	 * bucketed time-series of historical prices (datePrices), other entries
	 * submitted on the same day for the same food at the same eatery
	 * (communityEntries), and full details of the consensus entry.
	 *
	 * Endpoint: GET /api/food-entries/historical-data/{foodEntryId}?startDate=...
	 * Called by the frontend chart page to render: - Price-over-time chart (from
	 * datePrices) - "Others who reported" community section (from communityEntries)
	 * - Consensus entry details card (from consensusEntry)
	 *
	 * Community entry submitters are resolved via a single batch gRPC call to avoid
	 * N+1 round-trips to the user-service.
	 */
	public FoodEntryHistoricalDTO getFoodEntryHistoricalData(UUID foodEntryId, Instant startDate, UUID userId) {
		FoodEntry targetEntry = foodEntryRepo.findById(foodEntryId)
				.orElseThrow(() -> new RuntimeException("FoodEntry not found"));

		// Community entries: other price reports for the SAME (food, eatery) on the
		// same day
		LocalDate entryDate = targetEntry.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
		Instant dayStart = entryDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
		Instant dayEnd = entryDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
		List<FoodEntry> communityEntries = foodEntryRepo
				.findByFood_IdAndEatery_IdAndCreatedAtBetween(targetEntry.getFood().getId(),
						targetEntry.getEatery().getId(), dayStart, dayEnd)
				.stream().filter(e -> !e.getId().equals(foodEntryId)).toList();

		// Batch-fetch usernames for all community entry submitters (avoids N+1 gRPC
		// calls)
		Map<UUID, String> communityUsernames = fetchUsernamesBatch(
				communityEntries.stream().map(FoodEntry::getSubmitterId).distinct().toList());

		// Batch-fetch current user's votes for community entries
		Map<UUID, Boolean> userVotes = new HashMap<>();
		if (userId != null) {
			List<UUID> entryIds = communityEntries.stream().map(FoodEntry::getId).toList();
			if (!entryIds.isEmpty()) {
				foodEntryVoteRepo.findByVoterIdAndFoodEntryIdIn(userId, entryIds)
						.forEach(v -> userVotes.put(v.getFoodEntry().getId(), v.isUpvote()));
			}
		}

		List<FoodEntryPreviewDTO> otherEntriesOnSameDay = communityEntries.stream()
				.map(e -> new FoodEntryPreviewDTO(e.getId(), e.getFood().getLabel(), e.getSgCents(), e.getUpvoteCount(),
						e.getDownvoteCount(), null, // photo is redundant — all entries share the same food photo
						e.getSubmitterId(), communityUsernames.get(e.getSubmitterId()), e.getCreatedAt(),
						userVotes.get(e.getId())))
				.toList();

		List<DatePrice> datePrices = computeDatePrices(targetEntry.getFood().getId(), targetEntry.getEatery().getId(),
				startDate);

		var reqUserDetails = UserDetailsRequest.newBuilder().setUserId(targetEntry.getSubmitterId().toString()).build();
		UserDetailsResponse resUserDetails = userServiceStub.getUserDetails(reqUserDetails);
		String submitterUsername = resUserDetails.getUsername();

		return FoodEntryHistoricalDTO.builder().foodName(targetEntry.getFood().getLabel())
				.sgCentsConsensusPrice(targetEntry.getSgCents()).eateryId(targetEntry.getEatery().getId())
				.eateryAddress(targetEntry.getEatery().getAddress()).submitterUsername(submitterUsername)
				.datePrices(datePrices) // For the "historical data graph"
				.communityEntries(otherEntriesOnSameDay) // For the "community entries" (duh) section
				.consensusEntry(toDetailed(targetEntry)) // The one that will be shown on the "eatery panel" on the
				// right
				.build();
	}

	public FoodEntryDetailedDTO getFoodEntryDetailed(UUID foodEntryId) {
		FoodEntry entry = foodEntryRepo.findById(foodEntryId)
				.orElseThrow(() -> new RuntimeException("FoodEntry not found"));
		return toDetailed(entry);
	}

	public void submitFoodEntry(UUID submitterId, FoodEntrySubmissionRequest req) {
		// Use references....no need to fetch the entire object
		Eatery eateryRef = eateryRepo.getReferenceById(req.eateryId());
		Food foodRef = foodRepo.getReferenceById(req.foodId());

		// Build entity using the proxies
		FoodEntry entry = FoodEntry.builder().food(foodRef).eatery(eateryRef).sgCents(req.priceSgCents()).upvoteCount(0)
				.downvoteCount(0).submitterId(submitterId).build();

		// Triggers only a single INSERT query
		foodEntryRepo.save(entry);

		// Publish event so user-service can recalculate submitter's WTF
		wtfEventPublisher.publishEntryCreated(submitterId);
	}

	public void castVote(UUID voterId, UUID foodEntryId, Boolean isUpvote) {
		FoodEntry entry = foodEntryRepo.findById(foodEntryId)
				.orElseThrow(() -> new RuntimeException("FoodEntry not found"));

		if (entry.getSubmitterId().equals(voterId)) {
			throw new RuntimeException("Cannot vote on your own entry");
		}

		Optional<FoodEntryVote> existingVote = foodEntryVoteRepo.findByVoterIdAndFoodEntryId(voterId, foodEntryId);

		if (existingVote.isPresent()) {
			FoodEntryVote voteEntity = existingVote.get();

			if (isUpvote == null) {
				// Retract: delete the vote — DB trigger decrements the old type's counter
				VoteType removedType = voteEntity.isUpvote() ? VoteType.UPVOTE_REMOVED : VoteType.DOWNVOTE_REMOVED;
				foodEntryVoteRepo.deleteById(voteEntity.getId());
				wtfEventPublisher.publishVoteCast(voterId, entry.getSubmitterId(), removedType);
				return;
			}

			if (voteEntity.isUpvote() == isUpvote) {
				// Same vote again → idempotent no-op
				return;
			}

			// Toggle: delete old, reinsert new — DB trigger handles count deltas
			VoteType toggleType = isUpvote ? VoteType.DOWNVOTE_TO_UPVOTE : VoteType.UPVOTE_TO_DOWNVOTE;
			foodEntryVoteRepo.deleteById(voteEntity.getId());
			foodEntryVoteRepo
					.save(FoodEntryVote.builder().foodEntry(entry).voterId(voterId).isUpvote(isUpvote).build());
			wtfEventPublisher.publishVoteCast(voterId, entry.getSubmitterId(), toggleType);
			return;
		}

		// No existing vote
		if (isUpvote == null) {
			return;
		}

		VoteType voteType = isUpvote ? VoteType.UPVOTE : VoteType.DOWNVOTE;
		foodEntryVoteRepo.save(FoodEntryVote.builder().foodEntry(entry).voterId(voterId).isUpvote(isUpvote).build());
		wtfEventPublisher.publishVoteCast(voterId, entry.getSubmitterId(), voteType);
	}

	/**
	 * Find food entries within a bounding box, deduplicated by (eatery, food) —
	 * keeping the entry with the highest confidence score per group.
	 *
	 * Caching: Results are cached by coordinate-bucketed key to avoid repeated
	 * confidence computations during map panning.
	 */
	@Cacheable(value = "food_entries_bbox", keyGenerator = "bboxKeyGenerator")
	public List<FoodEntryMapDTO> findFoodEntriesWithinBounds(double minLat, double maxLat, double minLon,
			double maxLon) {
		List<Object[]> rows = foodEntryRepo.findWithinBoundsWithEateryLocation(minLat, maxLat, minLon, maxLon);

		// Deduplicate by (eateryId, foodName), keeping highest confidence
		Map<String, FoodEntry> bestByEateryFood = new LinkedHashMap<>();
		Map<String, Double> bestConfidenceByEateryFood = new LinkedHashMap<>();

		for (Object[] row : rows) {
			UUID entryId = (UUID) row[0];
			FoodEntry entry = foodEntryRepo.findById(entryId).orElse(null);
			if (entry == null)
				continue;

			UUID eateryId = (UUID) row[3];
			String foodName = (String) row[1];
			String key = eateryId + ":" + foodName;

			double confidence = confidenceAlgo.computeFinalConfidence(entry);
			Double currentBest = bestConfidenceByEateryFood.get(key);
			if (currentBest == null || confidence > currentBest) {
				bestConfidenceByEateryFood.put(key, confidence);
				bestByEateryFood.put(key, entry);
			}
		}

		List<FoodEntryMapDTO> result = new ArrayList<>();
		for (Map.Entry<String, FoodEntry> e : bestByEateryFood.entrySet()) {
			FoodEntry entry = e.getValue();
			String key = e.getKey();
			UUID eateryId = UUID.fromString(key.split(":")[0]);

			// Find the matching row for location data
			Object[] locationRow = rows.stream()
					.filter(r -> ((UUID) r[3]).equals(eateryId) && ((String) r[1]).equals(entry.getFood().getLabel()))
					.findFirst().orElse(null);

			if (locationRow != null) {
				result.add(new FoodEntryMapDTO(entry.getId(), entry.getFood().getLabel(), entry.getSgCents(), eateryId,
						(String) locationRow[4], ((Number) locationRow[5]).doubleValue(),
						((Number) locationRow[6]).doubleValue()));
			}
		}

		return result;
	}

	/**
	 * Buckets historical food entries (same food + eatery) into time intervals and
	 * picks the highest-confidence entry per bucket as the representative price.
	 *
	 * Granularity adapts to the actual data span (min createdAt → max createdAt):
	 * ≤30 days → daily buckets ≤180 days → weekly buckets (Monday) >180 days →
	 * monthly buckets (1st)
	 *
	 * Used by: getFoodEntryHistoricalData() to build the price-over-time chart. The
	 * caller provides startDate for the DB query; the actual data span determines
	 * the bucketing granularity (not startDate → now), so sparse old data is not
	 * collapsed into one bucket when the user picks a wide range.
	 */
	private List<DatePrice> computeDatePrices(UUID foodId, UUID eateryId, Instant startDate) {
		List<FoodEntry> entries = foodEntryRepo.findHistoricalEntriesWithVotes(foodId, eateryId, startDate);
		if (entries.isEmpty()) {
			return List.of();
		}

		long totalDays = Duration.between(entries.get(0).getCreatedAt(), entries.get(entries.size() - 1).getCreatedAt())
				.toDays();
		if (totalDays < 1)
			totalDays = 1;
		ZoneId zone = ZoneId.systemDefault();

		Map<LocalDate, List<FoodEntry>> bucketed = new LinkedHashMap<>();
		for (FoodEntry entry : entries) {
			LocalDate bucketDate = entry.getCreatedAt().atZone(zone).toLocalDate();
			if (totalDays > 180) {
				bucketDate = bucketDate.withDayOfMonth(1);
			} else if (totalDays > 30) {
				bucketDate = bucketDate.with(DayOfWeek.MONDAY);
			}
			bucketed.computeIfAbsent(bucketDate, k -> new ArrayList<>()).add(entry);
		}

		// For each bucket, pick the entry with the highest confidence as the
		// representative price
		List<DatePrice> result = new ArrayList<>();
		for (Map.Entry<LocalDate, List<FoodEntry>> bucket : bucketed.entrySet()) {
			FoodEntry best = null;
			double bestConf = -1;
			for (FoodEntry e : bucket.getValue()) {
				double conf = confidenceAlgo.computeFinalConfidence(e);
				if (conf > bestConf) {
					bestConf = conf;
					best = e;
				}
			}
			if (best != null) {
				result.add(new DatePrice(bucket.getKey().atStartOfDay(zone).toInstant(), best.getSgCents(), bestConf,
						bucket.getValue().size()));
			}
		}

		return result;
	}

	/**
	 * Batch-fetches usernames for a list of user IDs via a single gRPC call.
	 *
	 * Used by: getFoodEntryHistoricalData() to resolve submitter usernames for
	 * community entries without N+1 round-trips to the user-service.
	 */
	private Map<UUID, String> fetchUsernamesBatch(List<UUID> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		UserDetailsBatchRequest req = UserDetailsBatchRequest.newBuilder()
				.addAllUserIds(userIds.stream().map(UUID::toString).toList()).build();
		UserDetailsBatchResponse res = userServiceStub.getUserDetailsBatch(req);
		Map<UUID, String> map = new HashMap<>();
		for (UserDetailsBatchResponseItem item : res.getItemsList()) {
			map.put(UUID.fromString(item.getUserId()), item.getUsername());
		}
		return map;
	}

	/**
	 * Converts a FoodEntry entity into a FoodEntryDetailedDTO with: - Presigned
	 * photo URL (from MinIO) - Submitter profile info (from user-service via gRPC)
	 * - Submitter's total entry count (from foodEntryRepo)
	 *
	 * Used by: getFoodEntryHistoricalData() (consensusEntry field),
	 * getFoodEntryDetailed() (standalone detail endpoint).
	 */
	private FoodEntryDetailedDTO toDetailed(FoodEntry entry) {
		UUID submitterId = entry.getSubmitterId();
		long entriesSubmitted = submitterId == null ? 0L : foodEntryRepo.countBySubmitterId(submitterId);

		String foodPhotoPresignedUrl = minioService.getPresignedUrl(entry.getFood().getPhotoObjKey(),
				Duration.ofMinutes(30));

		assert submitterId != null;
		var userDetailsRequest = UserDetailsRequest.newBuilder().setUserId(submitterId.toString()).build();
		UserDetailsResponse userDetailsResponse = userServiceStub.getUserDetails(userDetailsRequest);

		return FoodEntryDetailedDTO.builder().foodEntryId(entry.getId()).foodPhotoPresignedUrl(foodPhotoPresignedUrl)
				.submittedAt(entry.getCreatedAt()).submitterId(submitterId)
				.submitterUsername(userDetailsResponse.getUsername())
				.submitterProfilePhotoPresignedUrl(userDetailsResponse.getPhotoUrl())
				.submitterWtfScore(userDetailsResponse.getWtfScore())
				.submitterTenureDays(userDetailsResponse.getTenureDays()).submitterEntriesSubmitted(entriesSubmitted)
				.build();
	}
}
