package com.salob.food_service.api.google;

import com.salob.food_service.api.google.dto.PlacesPhoto;
import com.salob.food_service.api.google.dto.PlacesSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

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

	public byte[] tryFindPhoto(String query) {
		log.info("Trying to find photo reference for query {}", query);
		String photoRef = findFirstPhotoReference(query);
		if (photoRef == null)
			return null;

		log.info("Found photoRef, downloading... {}", photoRef);
		byte[] photoBytes;
		try {
			photoBytes = downloadPhoto(photoRef);
		} catch (Exception e) {
			throw new RuntimeException("Failed to download photo reference for query " + query, e);
		}

		log.info("Downloaded photo bytes success!");
		return photoBytes;
	}

	private String findFirstPhotoReference(String query) {
		String encoded = URLEncoder.encode(query, UTF_8);
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

	private byte[] downloadPhoto(String photoReference) throws IOException {
		String urlStr = PHOTO_URL + "?key=" + URLEncoder.encode(apiKey, UTF_8) + "&photoreference="
				+ URLEncoder.encode(photoReference, UTF_8) + "&maxwidth=800";

		var conn = (HttpURLConnection) new URL(urlStr).openConnection();
		conn.setInstanceFollowRedirects(true);
		conn.connect();

		int status = conn.getResponseCode();
		String contentType = conn.getContentType();

		log.info("Photo download: status={}, contentType={}, size={}", status, contentType, conn.getContentLength());

		if (status != 200 || contentType == null || !contentType.startsWith("image/")) {
			log.warn("Unexpected photo response, skipping");
			return null;
		}

		byte[] bytes;
		try (var in = conn.getInputStream()) {
			bytes = in.readAllBytes();
		}

		// Validate JPEG header
		if (bytes.length < 3 || bytes[0] != (byte) 0xFF || bytes[1] != (byte) 0xD8 || bytes[2] != (byte) 0xFF) {
			log.warn("Response is not a valid JPEG (size={}, firstBytes={} {} {})", bytes.length, bytes[0], bytes[1],
					bytes[2]);
			return null;
		}

		log.info("Downloaded {} bytes of valid JPEG", bytes.length);
		return bytes;
	}
}
