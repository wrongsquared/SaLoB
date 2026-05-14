package com.salob.food_service.features.food.dto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 *
 * @param foodName - The foodName of the food item
 * @param sgCentsConsensusPrice - Using our 'Confidence' algorithm, the price which is most like
 *                                to be the correct one. (Is included within the 'pricePoints' array)
 * @param eateryAddress - The address of the eatery for this food entry
 * @param availableDates - Dates that contain food entries for this food + eatery
 * @param benchmarkDateEntries - Entries submitted on the consensus entry's date
 */
@Builder
public record FoodEntryHistoricalDTO(
    String foodName,
    int sgCentsConsensusPrice,
    UUID eateryId,
    String eateryAddress,
    List<LocalDate> availableDates,
    List<FoodEntryPreviewDTO> benchmarkDateEntries
) {}
