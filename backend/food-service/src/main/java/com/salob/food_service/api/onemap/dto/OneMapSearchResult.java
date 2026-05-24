package com.salob.food_service.api.onemap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OneMapSearchResult(@JsonProperty("SEARCHVAL") String searchVal, @JsonProperty("BLK_NO") String blkNo,
		@JsonProperty("ROAD_NAME") String roadName, @JsonProperty("BUILDING") String building,
		@JsonProperty("ADDRESS") String address, @JsonProperty("POSTAL") String postal,
		@JsonProperty("LATITUDE") String latitude, @JsonProperty("LONGITUDE") String longitude) {
}
