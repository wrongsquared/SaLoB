package com.salob.food_service.features.food.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FoodEntryDetailedDTO(
    UUID foodEntryId,
    String foodPhotoPresignedUrl,
    Instant submittedAt,

    // Submitter Details (for display on the "historical data" graph page)
    UUID submitterId,
    String submitterUsername,
    String submitterProfilePhotoPresignedUrl,
    Double submitterWtfScore,
    long submitterTenureDays,
    long submitterEntriesSubmitted
) {}

