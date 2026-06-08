package com.salob.food_service.api.google.dto;

import java.util.List;

public record PlacesTextSearchResponse(List<PlacesResult> results, String status) {
}
