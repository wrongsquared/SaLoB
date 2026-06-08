package com.salob.food_service.api.google.dto;

import java.util.List;

public record PlacesResult(String name, String formattedAddress, List<PlacesPhoto> photos) {
}
