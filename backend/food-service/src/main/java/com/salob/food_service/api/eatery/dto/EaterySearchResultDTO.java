package com.salob.food_service.api.eatery.dto;

import java.util.List;

public record EaterySearchResultDTO(List<EateryPreviewDTO> local, List<OneMapEateryDTO> onemap) {
}
