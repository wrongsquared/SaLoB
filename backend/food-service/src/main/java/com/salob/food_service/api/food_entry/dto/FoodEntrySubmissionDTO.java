package com.salob.food_service.api.food_entry.dto;

import java.util.UUID;

// TODO: Requires the API gateway to handle auth and inject user ID here
public record FoodEntrySubmissionDTO(
        UUID eateryId,
        UUID foodId,
        UUID submitterId,
        int priceSgCents
) {}
