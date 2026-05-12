package com.salob.food_service.features.eatery.dto;

import java.util.UUID;

public record FoodEntryPreviewDTO(
        UUID foodEntryId,
        String name,
        int sgCents,
        int upvotes,
        int downvotes
) {}
