package com.salob.food_service.api.eatery;

import com.salob.food_service.api._domain.Eatery;
import com.salob.food_service.api._domain.EateryClosureFlag;
import com.salob.food_service.api._domain.EateryType;
import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api._exceptions.EateryNotFoundException;
import com.salob.food_service.api.eatery.dto.EateryDetailedDTO;
import com.salob.food_service.api.eatery.dto.EateryMapDTO;
import com.salob.food_service.api.eatery.dto.EateryPreviewDTO;
import com.salob.food_service.api.eatery.dto.EaterySearchResultDTO;
import com.salob.food_service.api.eatery.dto.OneMapEateryDTO;
import com.salob.food_service.api.eatery_type.EateryTypeRepository;
import com.salob.food_service.api.food_entry.dto.FoodEntryPreviewDTO;
import com.salob.food_service.api.food_entry_vote.FoodEntryVoteRepository;
import com.salob.food_service.api.google.GooglePlacesClient;
import com.salob.food_service.api.onemap.OneMapClient;
import com.salob.food_service.api.onemap.dto.OneMapSearchResult;
import com.salob.food_service.common.ConfidenceAlgorithm;
import com.salob.food_service.common.Utils;
import com.salob.food_service.seeding.SeedImageHelper;
import com.salob.food_service.storage.minio.MinioStorageService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for eatery business logic.
 *
 * Services in Spring encapsulate business rules and coordinate multiple
 * components. This service handles: 1. Geospatial queries (delegated to
 * repository) 2. Caching layer (declared with @Cacheable) 3. Data mapping
 * (Eatery entity -> EateryMapDto)
 *
 * The @Service annotation tells Spring to manage this as a singleton bean,
 * making it injectable into controllers and other services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EateryService {

	private final EateryRepository eateryRepo;
	private final EateryClosureFlagRepository closureFlagRepo;
	private final EateryTypeRepository eateryTypeRepo;
	private final ConfidenceAlgorithm confidenceAlgorithm;
	private final MinioStorageService minioStorageService;
	private final OneMapClient oneMapClient;
	private final GooglePlacesClient googlePlacesClient;
	private final FoodEntryVoteRepository foodEntryVoteRepo;
	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

	public Eatery findById(UUID id) {
		return eateryRepo.findById(id).orElseThrow(() -> new EateryNotFoundException(id));
	}

	public List<EateryPreviewDTO> findNearestKEateriesWithinRadius(double lat, double lon, double radiusMeters, int k) {
		return eateryRepo.findNearestKEateriesWithinRadius(lat, lon, radiusMeters, k).stream().map(row -> {
			UUID eateryId = (UUID) row[0];
			String name = (String) row[1];
			String address = (String) row[2];
			return new EateryPreviewDTO(eateryId, name, address);
		}).toList();
	}

	/**
	 * Find eateries within a bounding box, with intelligent caching.
	 *
	 * CACHING STRATEGY (learn why this matters):
	 * ===================================================== Problem: User pans map
	 * continuously. Each pixel drag = slightly different bbox. Without bucketing,
	 * nearly every request = new cache key = DB hit.
	 *
	 * Solution: COORDINATE BUCKETING - Round bbox to 0.01° grid (about 1km
	 * precision, imperceptible on map) - All requests to similar areas → same cache
	 * key → cache hit - Trade-off: small precision loss for massive cache
	 * improvements
	 *
	 * Example: Request 1: bbox=[1.2700, 1.3200, 103.8000, 103.8600] → bucket
	 * key=[1.27, 1.32, 103.80, 103.86] Request 2: bbox=[1.2705, 1.3198, 103.8001,
	 * 103.8599] → SAME bucket key (cache hit!) Request 3: bbox=[1.2800, 1.3300,
	 * 103.9000, 103.9600] → different bucket key (cache miss, but new area)
	 *
	 * @param minLat
	 *            southern boundary
	 * @param maxLat
	 *            northern boundary
	 * @param minLon
	 *            western boundary
	 * @param maxLon
	 *            eastern boundary
	 * @return list of eateries, pulled from cache or DB
	 */
	@Cacheable(value = "eateries_bbox", keyGenerator = "bboxKeyGenerator")
	public List<EateryMapDTO> findEateriesWithinBounds(double minLat, double maxLat, double minLon, double maxLon) {
		// This log only appears on CACHE MISS (method actually executes)
		// log.info("=== CACHE MISS ===");
		// log.info("Querying database for bbox=[{}, {}, {}, {}]", minLat, maxLat,
		// minLon, maxLon);

		try {
			// Query database for eateries within bounds
			// log.debug("Executing PostGIS query...");
			List<Object[]> rows = eateryRepo.findWithinBoundingBox(minLat, maxLat, minLon, maxLon);
			// log.debug("Query returned {} rows", rows.size());

			// IMPORTANT: Use ArrayList (mutable), NOT .toList()
			// (ImmutableCollections$ListN)
			// Jackson can deserialize ArrayList but NOT internal immutable list types
			List<EateryMapDTO> result = new ArrayList<>();
			for (Object[] row : rows) {
				result.add(convertRowToEateryMapDto(row));
			}

			// log.info("Found {} eateries, about to cache result", result.size());
			// log.debug("Result type: {}, Result class: {}", result.getClass().getName(),
			// result.getClass().getSimpleName());

			// Note: @Cacheable will now try to serialize this result
			// If serialization fails, an exception will be thrown after this method returns
			return result;
		} catch (Exception e) {
			log.error("ERROR in findEateriesWithinBounds", e);
			throw e;
		}
	}

	/**
	 * This endpoint is meant to be used for the collapsible left-panel that expands
	 * when you click on an eatery on the map.
	 *
	 * For some eatery (eateryId), get its detailed information AND list of the
	 * "best" food entries for each food it serves. E.g An eatery may have 23
	 * entries for "chicken rice", but you want to show the one with the highest
	 * confidence
	 */
	public EateryDetailedDTO getEateryDetailed(UUID eateryId, UUID userId) {
		Eatery eatery = findById(eateryId);

		// Fetch user's votes for this eatery's entries before building previews
		Map<UUID, Boolean> userVotes = new HashMap<>();
		if (userId != null) {
			List<UUID> allEntryIds = eatery.getFoodEntries().stream().map(FoodEntry::getId).toList();
			if (!allEntryIds.isEmpty()) {
				foodEntryVoteRepo.findByVoterIdAndFoodEntryIdIn(userId, allEntryIds)
						.forEach(v -> userVotes.put(v.getFoodEntry().getId(), v.isUpvote()));
			}
		}

		Map<String, FoodEntryPreviewDTO> bestByFoodName = new LinkedHashMap<>();
		Map<String, Double> bestConfidenceByFoodName = new LinkedHashMap<>();
		for (FoodEntry foodEntry : eatery.getFoodEntries()) {
			String foodName = foodEntry.getFood().getLabel();
			double confidence = confidenceAlgorithm.getFinalConfidence(foodEntry);
			Double currentBest = bestConfidenceByFoodName.get(foodName);
			if (currentBest == null || confidence > currentBest) {
				bestConfidenceByFoodName.put(foodName, confidence);
				bestByFoodName.put(foodName,
						new FoodEntryPreviewDTO(foodEntry.getId(), foodName, foodEntry.getSgCents(),
								foodEntry.getUpvoteCount(), foodEntry.getDownvoteCount(),
								minioStorageService.getPresignedUrl(foodEntry.getFood().getPhotoObjKey(),
										Duration.ofMinutes(30)),
								foodEntry.getSubmitterId(), null, foodEntry.getCreatedAt(),
								userVotes.get(foodEntry.getId())));
			}
		}

		List<FoodEntryPreviewDTO> foodPreviews = new ArrayList<>(bestByFoodName.values());
		return new EateryDetailedDTO(eatery.getId(), eatery.getName(), eatery.getAddress(), eatery.getType().getLabel(),
				minioStorageService.getPresignedUrl(eatery.getPhotoObjKey(), Duration.ofMinutes(30)), foodPreviews);
	}

	@Cacheable(key = "#search.toLowerCase()", value = "eateries_search")
	public List<EateryPreviewDTO> searchForEateries(String search) {
		return eateryRepo.findBySearchCaseInsensitive(search).stream().map(this::convertRowToPreviewDto)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@Cacheable(key = "#search.toLowerCase()", value = "eateries_search_combined")
	public EaterySearchResultDTO searchCombined(String search) {
		List<EateryPreviewDTO> local = searchForEateries(search);

		List<OneMapEateryDTO> onemap;
		try {
			var response = oneMapClient.search(search);
			onemap = response.results() != null
					? response.results().stream().map(this::convertOneMapResultToDto).toList()
					: List.of();
		} catch (Exception e) {
			log.warn("OneMap search failed for '{}': {}", search, e.getMessage());
			onemap = List.of();
		}

		return new EaterySearchResultDTO(local, onemap);
	}

	@Transactional
	public Eatery createEatery(String name, String address, UUID typeId) {
		Optional<Eatery> existing = eateryRepo.findByName(name);
		if (existing.isPresent()) {
			return existing.get();
		}

		EateryType type = eateryTypeRepo.findById(typeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid eatery type"));

		double[] coords = oneMapClient.geocode(address);
		Point location = geometryFactory.createPoint(new Coordinate(coords[1], coords[0]));

		// Successfully geocoded, now try to fetch photo from Google Places API...
		String photoObjKey = null;
		String strippedAddress = Utils.stripPostalCode(address);

		try {
			byte[] photoBytes = googlePlacesClient.fetchPhotoBytes(strippedAddress);
			if (photoBytes != null) {
				photoObjKey = minioStorageService.uploadBytes(photoBytes,
						SeedImageHelper.EATERY_PREFIX + "/" + Utils.prepareAddressForObjKey(strippedAddress) + ".jpg",
						"image/jpeg");
			}
		} catch (Exception e) {
			log.warn("Failed to fetch photo for address '{}' using Places API: {}", strippedAddress, e.getMessage());
		}

		Eatery eatery = Eatery.builder().name(name).address(address).location(location).type(type).isOpen(true)
				.photoObjKey(photoObjKey).build();
		return eateryRepo.save(eatery);
	}

	/**
	 * Report an eatery as closed. Idempotent — same user cannot flag the same
	 * eatery twice.
	 *
	 * TODO: Future AI verification pipeline — flag goes to moderation queue, AI
	 * checks closure legitimacy (e.g., cross-references with OneMap/Google Places),
	 * then auto-approves or escalates to human moderator.
	 */
	public void reportClosed(UUID eateryId, UUID flaggerId) {
		Eatery eatery = findById(eateryId);

		if (closureFlagRepo.existsByEateryIdAndFlaggerId(eateryId, flaggerId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already reported by this user");
		}

		EateryClosureFlag flag = EateryClosureFlag.builder().eatery(eatery).flaggerId(flaggerId).build();
		closureFlagRepo.save(flag);
		log.info("Eatery {} reported as closed by user {}", eateryId, flaggerId);
	}

	/**
	 * Convert a database query result row into a DTO.
	 *
	 * The repository returns Object[] because of the native SQL query. We need to
	 * manually map columns to the DTO record.
	 *
	 * @param row
	 *            raw database result (e.g., [eateryId, foodName, lat, lon,
	 *            typeLabel, isClosed])
	 * @return strongly-typed DTO
	 */
	private EateryMapDTO convertRowToEateryMapDto(Object[] row) {
		return new EateryMapDTO((UUID) row[0], // eateryId
				(String) row[1], // foodName
				((Number) row[2]).doubleValue(), // latitude (ST_Y as Double)
				((Number) row[3]).doubleValue(), // longitude (ST_X as Double)
				(String) row[4] // typeLabel
		);
	}

	private EateryPreviewDTO convertRowToPreviewDto(Object[] row) {
		return new EateryPreviewDTO((UUID) row[0], (String) row[1], (String) row[2]);
	}

	private OneMapEateryDTO convertOneMapResultToDto(OneMapSearchResult result) {
		return new OneMapEateryDTO(deriveName(result), result.address());
	}

	private String deriveName(OneMapSearchResult result) {
		if (result.building() != null && !result.building().isBlank()) {
			return result.building();
		}
		String blk = result.blkNo() != null ? result.blkNo().trim() : "";
		String road = result.roadName() != null ? result.roadName().trim() : "";
		if (!blk.isBlank() && !road.isBlank()) {
			return "Blk " + blk + " " + road;
		}
		return result.address();
	}
}
