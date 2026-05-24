package com.salob.food_service.api.eatery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEateryRequest(@NotBlank String name, @NotBlank String address, @NotNull UUID typeId) {
}
