package com.salob.food_service.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FoodCreationRequest(
        @NotBlank(message = "Food name is required")
        @Size(max = 100, message = "Food name must be 100 characters or less")
        String foodName
) {}
