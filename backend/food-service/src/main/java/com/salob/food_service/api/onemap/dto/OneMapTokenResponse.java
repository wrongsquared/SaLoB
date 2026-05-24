package com.salob.food_service.api.onemap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OneMapTokenResponse(@JsonProperty("access_token") String accessToken,
		@JsonProperty("expiry_timestamp") String expiryTimestamp) {
}
