package com.salob.food_service.api.eatery.dto;

import java.util.UUID;

public record EateryPreviewDTO(
        UUID eateryId,
        String name,
        String address
) {}
