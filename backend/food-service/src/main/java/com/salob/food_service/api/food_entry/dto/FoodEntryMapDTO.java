package com.salob.food_service.api.food_entry.dto;

import java.util.UUID;

/**
 * Lightweight DTO for food entries within a bounding box.
 * Used by the map view in food mode — one marker per unique food at each eatery.
 */
public record FoodEntryMapDTO(
        UUID foodEntryId,
        String foodName,
        int sgCents,
        UUID eateryId,
        String eateryName,
        double latitude,
        double longitude
) {}
