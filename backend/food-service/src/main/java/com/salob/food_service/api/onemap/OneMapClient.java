package com.salob.food_service.api.onemap;

import com.salob.food_service.api.onemap.dto.OneMapSearchResponse;
import com.salob.food_service.api.onemap.dto.OneMapSearchResult;
import com.salob.food_service.api.onemap.dto.OneMapTokenResponse;
import com.salob.food_service.common.Utils;
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
import java.util.regex.Pattern;

@Slf4j
@Component
public class OneMapClient {

	private static final Duration TOKEN_REFRESH_MARGIN = Duration.ofHours(1);
	public static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("SINGAPORE (\\d{6})$", Pattern.CASE_INSENSITIVE);

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

	// Given an address, try to 'geocode (get the search result)' using the
	// following strategies, in order...
	// 1) Postal code (if exists)
	// 2) Address WITHOUT the postal code
	// 3) Full address (fallback)
	public double[] geocode(String address) {
		log.info("Received request to geocode address {}", address);

		// Strategy 1: extract 6-digit postal code and search by it
		// Singapore postal codes are unique per building — most reliable hit.
		var postalMatcher = POSTAL_CODE_PATTERN.matcher(address);
		if (postalMatcher.find()) {
			String postalCode = postalMatcher.group(1);
			log.info("Extracted postal code {}, trying geocode by postal code", postalCode);
			OneMapSearchResponse postalResponse = search(postalCode);
			OneMapSearchResult result = extractFirstResult(postalResponse);
			if (result != null) {
				return parseCoordinates(result, address);
			}
		}

		// Strategy 2: strip "SINGAPORE XXXXXX" suffix and search the clean address
		String stripped = Utils.stripPostalCode(address);
		if (!stripped.isEmpty() && !stripped.equals(address)) {
			log.info("Stripped postal suffix to '{}', trying geocode", stripped);
			OneMapSearchResponse strippedResponse = search(stripped);
			OneMapSearchResult result = extractFirstResult(strippedResponse);
			if (result != null) {
				return parseCoordinates(result, address);
			}
		}

		// Strategy 3: fall back to the original full address
		log.info("Falling back to geocode with original address");
		OneMapSearchResponse fullResponse = search(address);
		OneMapSearchResult result = extractFirstResult(fullResponse);
		if (result != null) {
			return parseCoordinates(result, address);
		}

		throw new RuntimeException("OneMap geocode failed: no results for '" + address + "'");
	}

	private OneMapSearchResult extractFirstResult(OneMapSearchResponse response) {
		if (response.results() == null || response.results().isEmpty()) {
			return null;
		}
		return response.results().getFirst();
	}

	private double[] parseCoordinates(OneMapSearchResult result, String originalAddress) {
		try {
			double lat = Double.parseDouble(result.latitude());
			double lon = Double.parseDouble(result.longitude());
			return new double[]{lat, lon};
		} catch (NumberFormatException e) {
			throw new RuntimeException("OneMap geocode returned invalid coordinates for '" + originalAddress + "'", e);
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
