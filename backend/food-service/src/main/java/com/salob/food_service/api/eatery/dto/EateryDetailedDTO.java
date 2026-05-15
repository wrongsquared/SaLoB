package com.salob.food_service.api.eatery.dto;

import com.salob.food_service.api.food_entry.dto.FoodEntryPreviewDTO;

import java.util.List;
import java.util.UUID;

public record EateryDetailedDTO(
        UUID eateryId,
        String name,
        String address,
        String typeLabel,
        String photoUrl,
        List<FoodEntryPreviewDTO> foodPreviews
) {}
