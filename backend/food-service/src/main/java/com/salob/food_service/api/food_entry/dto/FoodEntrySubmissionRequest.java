package com.salob.food_service.api.food_entry.dto;

import java.util.UUID;

public record FoodEntrySubmissionRequest(
        UUID eateryId,
        UUID foodId,
        int priceSgCents
) {}
