package com.salob.food_service.api.food_entry.dto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Historical pricing data for a food entry.
 *
 * @param foodName - The name of the food item
 * @param sgCentsConsensusPrice - Using our 'Confidence' algorithm, the price most likely
 *                                to be correct. (Is included within the 'benchmarkDateEntries' array)
 * @param eateryId - The eatery UUID for this food entry
 * @param eateryAddress - The address of the eatery for this food entry
 * @param submitterUsername - Username of the submitter of the consensus entry
 * @param availableDates - Dates that contain food entries for this food + eatery
 * @param benchmarkDateEntries - Entries submitted on the consensus entry's date
 * @param consensusEntry - Full details of the consensus (best-confidence) entry
 */
@Builder
public record FoodEntryHistoricalDTO(
    String foodName,
    int sgCentsConsensusPrice,
    UUID eateryId,
    String eateryAddress,
    String submitterUsername,
    List<LocalDate> availableDates,
    List<FoodEntryPreviewDTO> benchmarkDateEntries,
    FoodEntryDetailedDTO consensusEntry
) {}
