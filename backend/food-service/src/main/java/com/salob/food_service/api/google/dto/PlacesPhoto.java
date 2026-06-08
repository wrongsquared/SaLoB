package com.salob.food_service.api.google.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlacesPhoto(@JsonProperty("photo_reference") String photoReference, int width, int height) {
}
