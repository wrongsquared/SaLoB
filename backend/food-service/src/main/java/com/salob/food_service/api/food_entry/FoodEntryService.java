package com.salob.food_service.api.food_entry;

import com.salob.food_service.api._domain.Eatery;
import com.salob.food_service.api._domain.Food;
import com.salob.food_service.api.eatery.EateryRepository;
import com.salob.food_service.api.food.FoodRepository;
import com.salob.food_service.api.food_entry.dto.FoodEntrySubmissionRequest;
import com.salob.food_service.common.ConfidenceAlgorithm;
import com.salob.food_service.api.food_entry.dto.FoodEntryDetailedDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryHistoricalDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryMapDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryPreviewDTO;
import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.storage.minio.MinioStorageService;
import com.salob.proto.user.UserDetailsRequest;
import com.salob.proto.user.UserDetailsResponse;
import com.salob.proto.user.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodEntryService {
    private final FoodEntryRepository foodEntryRepo;
    private final EateryRepository eateryRepo;
    private final FoodRepository foodRepo;

    private final ConfidenceAlgorithm confidenceAlgo;
    private final MinioStorageService minioService;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    /**
     * Given a 'foodEntryId', find all the other food entries of the same "food" from the same "eatery",
     * and then aggregate their price points.
     */
    public FoodEntryHistoricalDTO getFoodEntryHistoricalData(UUID foodEntryId, Instant startDate) {
        FoodEntry targetEntry = foodEntryRepo.findById(foodEntryId)
                .orElseThrow(() -> new RuntimeException("FoodEntry not found"));

        // Fetch all the same food from the same eatery as 'targetEntry', created after startDate
        List<FoodEntry> allEntriesOfSameFoodAndEatery = foodEntryRepo.findByFood_IdAndEatery_Id(
                targetEntry.getFood().getId(),
                targetEntry.getEatery().getId()
        ).stream()
                .filter(entry -> entry.getCreatedAt().isAfter(startDate) || entry.getCreatedAt().equals(startDate))
                .toList();

        // Calculate confidence for each entry and find the best (and concurrently, save the dates that there ARE entries)
        int consensusPrice = -1;
        double bestConfidence = -1.0;
        FoodEntry consensusEntry = null;

        Set<LocalDate> availableDates = new TreeSet<>();
        for (FoodEntry foodEntry : allEntriesOfSameFoodAndEatery) {
            double confidence = confidenceAlgo.computeFinalConfidence(foodEntry);

            LocalDate entryDate = foodEntry.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            availableDates.add(entryDate);

            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                consensusPrice = foodEntry.getSgCents();
                consensusEntry = foodEntry;
            }
        }

        // Collect all the entries from the date where the 'consensus entry' was created
        List<FoodEntryPreviewDTO> benchmarkDateEntries = new ArrayList<>();
        FoodEntryDetailedDTO consensusEntryDetails = null;
        if (consensusEntry != null) {
            LocalDate consensusDate = consensusEntry.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            Instant dayStart = consensusDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = consensusDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            List<FoodEntry> entriesOnConsensusDate = foodEntryRepo.findByEatery_IdAndCreatedAtBetween(
                targetEntry.getEatery().getId(),
                dayStart,
                dayEnd
            );

            for (FoodEntry entry : entriesOnConsensusDate) {
                String photoObjKey = entry.getFood().getPhotoObjKey();
                String presignedUrl = minioService.getPresignedUrl(photoObjKey, Duration.ofMinutes(30));
                benchmarkDateEntries.add(
                    new FoodEntryPreviewDTO(
                        entry.getId(),
                        entry.getFood().getLabel(),
                        entry.getSgCents(),
                        entry.getUpvoteCount(),
                        entry.getDownvoteCount(),
                        presignedUrl,
                        entry.getSubmitterId(),
                        null,
                        entry.getCreatedAt()
                    )
                );
            }
            consensusEntryDetails = toDetailed(consensusEntry);
        }

        // Return all the 'price points', with additional info for the point with the 'consensus price'
        return FoodEntryHistoricalDTO.builder()
                .foodName(targetEntry.getFood().getLabel())
                .sgCentsConsensusPrice(consensusPrice)
                .eateryId(targetEntry.getEatery().getId())
                .eateryAddress(targetEntry.getEatery().getAddress())
                .availableDates(new ArrayList<>(availableDates))
                .benchmarkDateEntries(benchmarkDateEntries)
                .consensusEntry(consensusEntryDetails)
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
        FoodEntry entry = FoodEntry.builder()
                .food(foodRef)
                .eatery(eateryRef)
                .sgCents(req.priceSgCents())
                .upvoteCount(0)
                .downvoteCount(0)
                .submitterId(submitterId)
                .build();

        // Triggers only a single INSERT query
        foodEntryRepo.save(entry);
    }

    /**
     * Find food entries within a bounding box, deduplicated by (eatery, food) —
     * keeping the entry with the highest confidence score per group.
     *
     * Caching: Results are cached by coordinate-bucketed key to avoid repeated
     * confidence computations during map panning.
     */
    @Cacheable(
        value = "food_entries_bbox",
        keyGenerator = "bboxKeyGenerator"
    )
    public List<FoodEntryMapDTO> findFoodEntriesWithinBounds(
            double minLat,
            double maxLat,
            double minLon,
            double maxLon
    ) {
        List<Object[]> rows = foodEntryRepo.findWithinBoundsWithEateryLocation(minLat, maxLat, minLon, maxLon);

        // Deduplicate by (eateryId, foodName), keeping highest confidence
        Map<String, FoodEntry> bestByEateryFood = new LinkedHashMap<>();
        Map<String, Double> bestConfidenceByEateryFood = new LinkedHashMap<>();

        for (Object[] row : rows) {
            UUID entryId = (UUID) row[0];
            FoodEntry entry = foodEntryRepo.findById(entryId).orElse(null);
            if (entry == null) continue;

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
                .findFirst()
                .orElse(null);

            if (locationRow != null) {
                result.add(new FoodEntryMapDTO(
                    entry.getId(),
                    entry.getFood().getLabel(),
                    entry.getSgCents(),
                    eateryId,
                    (String) locationRow[4],
                    ((Number) locationRow[5]).doubleValue(),
                    ((Number) locationRow[6]).doubleValue()
                ));
            }
        }

        return result;
    }

    private FoodEntryDetailedDTO toDetailed(FoodEntry entry) {
        UUID submitterId = entry.getSubmitterId();
        long entriesSubmitted = submitterId == null ? 0L : foodEntryRepo.countBySubmitterId(submitterId);

        String foodPhotoPresignedUrl = minioService.getPresignedUrl(
            entry.getFood().getPhotoObjKey(),
            Duration.ofMinutes(30)
        );

        assert submitterId != null;
        var userDetailsRequest = UserDetailsRequest.newBuilder()
            .setUserId(submitterId.toString())
            .build();
        UserDetailsResponse userDetailsResponse = userServiceStub.getUserDetails(userDetailsRequest);

        return FoodEntryDetailedDTO.builder()
            .foodEntryId(entry.getId())
            .foodPhotoPresignedUrl(foodPhotoPresignedUrl)
            .submittedAt(entry.getCreatedAt())
            .submitterId(submitterId)
            .submitterUsername(userDetailsResponse.getUsername())
            .submitterProfilePhotoPresignedUrl(userDetailsResponse.getPhotoUrl())
            .submitterWtfScore(userDetailsResponse.getWtfScore())
            .submitterTenureDays(userDetailsResponse.getTenureDays())
            .submitterEntriesSubmitted(entriesSubmitted)
            .build();
    }
}
