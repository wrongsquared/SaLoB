package com.salob.food_service.features.eatery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Data Transfer Object (DTO) for map view eatery data.
 *
 * DTOs are lightweight objects used to transfer data between layers (e.g., controller -> frontend).
 * Unlike the full Eatery entity, this DTO includes ONLY fields needed for the map UI,
 * keeping the response payload small and focused.
 *
 * Benefits:
 * - Smaller JSON response (faster network transfer, especially for 50+ eateries)
 * - API contract is explicit and stable (won't accidentally expose internal fields)
 * - Can evolve frontend needs without exposing DB schema
 */
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public final class EateryMapDto {
    UUID id;
    String name;
    double latitude;
    double longitude;
    String typeLabel;
    boolean isClosed;
}

