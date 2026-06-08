package com.salob.food_service.api.google;

import com.salob.food_service.api.google.dto.PlacesPhoto;
import com.salob.food_service.api.google.dto.PlacesSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class GooglePlacesClient {
	private static final String TEXT_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json";
	private static final String PHOTO_URL = "https://maps.googleapis.com/maps/api/place/photo";

	private final RestClient restClient;
	private final String apiKey;

	private GooglePlacesClient(@Value("${google.places-api-key}") String apiKey) {
		this.restClient = RestClient.create();
		this.apiKey = apiKey;
	}

	public byte[] fetchPhotoBytes(String query) {
		String photoRef = findFirstPhotoReference(query);
		if (photoRef == null)
			return null;
		return downloadPhoto(photoRef);
	}

	private String findFirstPhotoReference(String query) {
		String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String url = TEXT_SEARCH_URL + "?key=" + apiKey + "&query=" + encoded;

		var response = restClient.get().uri(url).retrieve().body(PlacesSearchResponse.class);
		if (response == null || response.results() == null || response.results().isEmpty()) {
			log.warn("Google Places text search returned no results for '{}'", query);
			return null;
		}

		List<PlacesPhoto> photos = response.results().getFirst().photos();
		if (photos == null || photos.isEmpty()) {
			log.warn("No photos for first result of '{}'", query);
			return null;
		}
		return photos.getFirst().photoReference();
	}

	private byte[] downloadPhoto(String photoReference) {
		String url = PHOTO_URL + "?key=" + apiKey + "&photoreference=" + photoReference + "&maxwidth=800";
		return restClient.get().uri(url).retrieve().body(byte[].class);
	}
}
