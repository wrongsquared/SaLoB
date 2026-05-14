package com.salob.food_service.features.eatery.dto;

import com.salob.food_service.features.food.dto.FoodEntryPreviewDTO;

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
