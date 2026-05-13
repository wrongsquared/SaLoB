package com.salob.food_service.features.food;

import com.salob.food_service.common.ConfidenceAlgorithm;
import com.salob.food_service.features.eatery.dto.FoodEntryHistoricalDTO;
import com.salob.food_service.features.eatery.dto.PricePoint;
import com.salob.food_service.features.food.domain.FoodEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodEntryService {
    private final FoodEntryRepository foodEntryRepo;
    private final ConfidenceAlgorithm confidenceAlgo;

    /**
     * Given a 'foodEntryId', find all the other food entries of the same "food" from the same "eatery",
     * and then aggregate their price points.
     */
    public FoodEntryHistoricalDTO getFoodEntryHistoricalData(UUID foodEntryId, Instant startDate) {
        FoodEntry targetEntry = foodEntryRepo.findById(foodEntryId)
                .orElseThrow(() -> new RuntimeException("FoodEntry not found"));

        // Fetch all 'price points' (a price at a point in time) for the same food from the same eatery, created after startDate
        List<FoodEntry> allEntriesOfSameFoodAndEatery = foodEntryRepo.findByFood_IdAndEatery_Id(
                targetEntry.getFood().getId(),
                targetEntry.getEatery().getId()
        ).stream()
                .filter(entry -> entry.getCreatedAt().isAfter(startDate) || entry.getCreatedAt().equals(startDate))
                .toList();

        // Calculate confidence for each entry and find the best
        int consensusPrice = -1;
        double bestConfidence = -1.0;

        List<PricePoint> pricePoints = new ArrayList<>();
        for (FoodEntry foodEntry : allEntriesOfSameFoodAndEatery) {
            double confidence = confidenceAlgo.computeFinalConfidence(foodEntry);

            LocalDate entryDate = foodEntry.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            pricePoints.add(new PricePoint(foodEntry.getSgCents(), entryDate));

            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                consensusPrice = foodEntry.getSgCents();
            }
        }

        // Return all the 'price points', with additional info for the point with the 'consensus price'
        return FoodEntryHistoricalDTO.builder()
                .name(targetEntry.getFood().getLabel())
                .sgCentsConsensusPrice(consensusPrice)
                .address(targetEntry.getEatery().getAddress())
                .pricePoints(pricePoints)
                .build();
    }
}
