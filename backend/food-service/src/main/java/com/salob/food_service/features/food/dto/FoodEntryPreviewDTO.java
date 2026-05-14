package com.salob.food_service.features.food.dto;

import java.time.Instant;
import java.util.UUID;

public record FoodEntryPreviewDTO(
        UUID foodEntryId,
        String name,
        int sgCents,
        int upvotes,
        int downvotes,
        String photoPresignedUrl,
        UUID submitterId,
        String submitterUsername,
        Instant createdAt
) {}
