package com.salob.food_service.api.onemap.dto;

import java.util.List;

public record OneMapSearchResponse(int found, int totalNumPages, int pageNum, List<OneMapSearchResult> results) {
}
