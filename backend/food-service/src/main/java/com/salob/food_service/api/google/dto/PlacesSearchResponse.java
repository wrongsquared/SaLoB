package com.salob.food_service.api.google.dto;

import java.util.List;

public record PlacesSearchResponse(String status, List<PlacesResult> results) {
}
