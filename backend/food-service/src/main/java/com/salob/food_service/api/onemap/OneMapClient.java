package com.salob.food_service.api.onemap;

import com.salob.food_service.api.onemap.dto.OneMapSearchResponse;
import com.salob.food_service.api.onemap.dto.OneMapSearchResult;
import com.salob.food_service.api.onemap.dto.OneMapTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class OneMapClient {

	private static final Duration TOKEN_REFRESH_MARGIN = Duration.ofHours(1);

	private final OneMapProperties properties;
	private final RestClient restClient;

	private String token;
	private Instant tokenExpiry;

	public OneMapClient(OneMapProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.create();
	}

	public OneMapSearchResponse search(String query) {
		ensureToken();
		String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String url = properties.getApiUrl() + "/api/common/elastic/search" + "?searchVal=" + encodedQuery
				+ "&returnGeom=Y" + "&getAddrDetails=Y" + "&pageNum=1";

		return restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve()
				.body(OneMapSearchResponse.class);
	}

	public double[] geocode(String address) {
		log.info("Received request to geocode address {}", address);
		OneMapSearchResponse response = search(address);
		log.info("Geocode response {}", response);
		if (response.results() == null || response.results().isEmpty()) {
			throw new RuntimeException("OneMap geocode failed: no results for '" + address + "'");
		}
		try {
			OneMapSearchResult firstResult = response.results().getFirst();
			double lat = Double.parseDouble(firstResult.latitude());
			double lon = Double.parseDouble(firstResult.longitude());
			return new double[]{lat, lon};
		} catch (NumberFormatException e) {
			throw new RuntimeException("OneMap geocode returned invalid coordinates for '" + address + "'", e);
		}
	}

	private void ensureToken() {
		if (token == null || tokenExpiry == null || Instant.now().isAfter(tokenExpiry)) {
			refreshToken();
		}
	}

	private void refreshToken() {
		String url = properties.getApiUrl() + "/api/auth/post/getToken";

		Map<String, String> payload = Map.of("email", properties.getEmail(), "password", properties.getPassword());

		OneMapTokenResponse tokenResponse = restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).body(payload).retrieve().body(OneMapTokenResponse.class);

		if (tokenResponse == null || tokenResponse.accessToken() == null) {
			throw new RuntimeException("Failed to obtain OneMap API token");
		}

		this.token = tokenResponse.accessToken();
		this.tokenExpiry = parseExpiry(tokenResponse.expiryTimestamp());
		log.info("OneMap token refreshed, expires at {}", tokenExpiry);
	}

	private Instant parseExpiry(String expiryTimestamp) {
		try {
			LocalDateTime expiry = LocalDateTime.parse(expiryTimestamp,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			return expiry.atZone(java.time.ZoneId.of("Asia/Singapore")).toInstant().minus(TOKEN_REFRESH_MARGIN);
		} catch (Exception e) {
			log.warn("Failed to parse OneMap token expiry '{}', defaulting to 3 days", expiryTimestamp);
			return Instant.now().plus(Duration.ofDays(3)).minus(TOKEN_REFRESH_MARGIN);
		}
	}
}
