package com.salob.food_service.api.eatery;

import com.salob.food_service.api.eatery.dto.EateryDetailedDTO;
import com.salob.food_service.api.eatery.dto.EateryMapDTO;
import com.salob.food_service.api.eatery.dto.EateryPreviewDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for eatery-related endpoints.
 *
 * <h2>When to use this controller</h2>
 * <p>
 * This controller serves the <strong>map-driven discovery flow</strong> of the
 * SaLoB platform. It is called by the frontend HomePage when a user is browsing
 * the interactive map, searching for eateries by name, or viewing detailed
 * information about a specific eatery.
 * </p>
 *
 * <h3>Typical frontend flow</h3>
 * <ol>
 * <li>User pans/zooms the map → frontend calls {@code GET /within-bounds} to
 * get markers</li>
 * <li>User clicks a marker → frontend calls {@code GET /{eateryId}} to show the
 * sidebar panel</li>
 * <li>User types in the search bar → frontend calls {@code GET /search} for
 * autocomplete results</li>
 * <li>User reports a closed eatery → frontend calls {@code POST
 * /{eateryId}/report-closed}</li>
 * </ol>
 *
 * <h3>Architecture notes</h3>
 * <ul>
 * <li>All endpoints are read-optimized with aggressive caching (see
 * {@link EateryService})</li>
 * <li>Geospatial queries use PostGIS {@code ST_Within} for efficient
 * bounding-box filtering</li>
 * <li>Rate limiting is handled at the API gateway layer</li>
 * </ul>
 *
 * <h3>Related controllers</h3>
 * <ul>
 * <li>{@link com.salob.food_service.api.food_entry.FoodEntryController} — for
 * submitting and voting on food prices</li>
 * <li>{@link com.salob.food_service.api.food.FoodController} — for food catalog
 * management</li>
 * </ul>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/eateries")
public class EateryController {

	private final EateryService eateryService;

	/**
	 * Fetch eateries within a bounding box for map marker rendering.
	 *
	 * <h3>When to use</h3>
	 * <p>
	 * Called by the frontend <strong>every time the user pans or zooms the
	 * map</strong>. The frontend computes the visible viewport bounds and sends
	 * them here to get a lightweight list of eateries (id, name, lat, lon, type)
	 * for rendering markers.
	 * </p>
	 *
	 * <h3>Performance characteristics</h3>
	 * <ul>
	 * <li>Results are cached using coordinate bucketing — panning within the same
	 * bucket returns cached data without hitting PostGIS</li>
	 * <li>Response payload is minimal (~50 bytes per eatery) to keep map
	 * interactions snappy</li>
	 * <li>Rate limiting is applied at the API gateway</li>
	 * </ul>
	 *
	 * <h3>Example</h3>
	 *
	 * <pre>
	 * GET /api/eateries/within-bounds?minLat=1.27&maxLat=1.32&minLon=103.80&maxLon=103.86
	 * → [{eateryId, name, latitude, longitude, typeLabel}, ...]
	 * </pre>
	 *
	 * @param minLat
	 *            minimum latitude (southern boundary)
	 * @param maxLat
	 *            maximum latitude (northern boundary)
	 * @param minLon
	 *            minimum longitude (western boundary)
	 * @param maxLon
	 *            maximum longitude (eastern boundary)
	 * @return list of eateries within the bounding box
	 */
	@GetMapping("/within-bounds")
	public ResponseEntity<List<EateryMapDTO>> getEateriesWithinBounds(@Valid @RequestParam double minLat,
			@Valid @RequestParam double maxLat, @Valid @RequestParam double minLon,
			@Valid @RequestParam double maxLon) {
		if (minLat >= maxLat || minLon >= maxLon) {
			return ResponseEntity.badRequest().build();
		}

		List<EateryMapDTO> eateries = eateryService.findEateriesWithinBounds(minLat, maxLat, minLon, maxLon);
		return ResponseEntity.ok(eateries);
	}

	/**
	 * Fetch full details for a single eatery (sidebar panel).
	 *
	 * <h3>When to use</h3>
	 * <p>
	 * Called when the user <strong>clicks a marker on the map</strong>. The
	 * frontend opens a sidebar panel showing the eatery's hero image, rating,
	 * address, and a list of food entries with prices.
	 * </p>
	 *
	 * <h3>Response includes</h3>
	 * <ul>
	 * <li>Eatery metadata: name, address, type, photo URL</li>
	 * <li>Food previews: top-voted entry per food name (deduplicated)</li>
	 * </ul>
	 *
	 * <h3>Example</h3>
	 *
	 * <pre>
	 * GET /api/eateries/fg233ae7-a797-48b8-b6ee-c0ecc4a983e8
	 * → {eateryId, name, address, typeLabel, photoUrl, foodPreviews: [...]}
	 * </pre>
	 *
	 * @param eateryId
	 *            the eatery UUID
	 * @return detailed eatery information with food previews
	 */
	@GetMapping("/{eateryId}")
	public ResponseEntity<EateryDetailedDTO> getEateryDetailed(@Valid @PathVariable UUID eateryId) {
		return ResponseEntity.ok(eateryService.getEateryDetailed(eateryId));
	}

	/**
	 * Search eateries by name (autocomplete).
	 *
	 * <h3>When to use</h3>
	 * <p>
	 * Called by the frontend <strong>search bar as the user types</strong>. Returns
	 * matching eateries with name and address for display in a dropdown.
	 * </p>
	 *
	 * <h3>Example</h3>
	 *
	 * <pre>
	 * GET /api/eateries/search?search=bedok
	 * → [{eateryId, name, address}, ...]
	 * </pre>
	 *
	 * @param search
	 *            the search query (partial eatery name)
	 * @return matching eateries, or 204 No Content if none found
	 */
	@GetMapping("/search")
	public ResponseEntity<List<EateryPreviewDTO>> searchForEateries(@Valid @RequestParam String search) {
		List<EateryPreviewDTO> results = eateryService.searchForEateries(search);
		if (results.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(results);
	}

	/**
	 * Report an eatery as permanently closed.
	 *
	 * <h3>When to use</h3>
	 * <p>
	 * Called when a user <strong>flags an eatery as no longer operating</strong>.
	 * This is a community moderation feature — multiple reports may trigger admin
	 * review or automatic closure status.
	 * </p>
	 *
	 * <h3>Authentication</h3>
	 * <p>
	 * Requires {@code X-User-Id} header identifying the reporting user.
	 * </p>
	 *
	 * <h3>Example</h3>
	 *
	 * <pre>
	 * POST /api/eateries/{eateryId}/report-closed
	 * Header: X-User-Id: 550e8400-e29b-41d4-a716-446655440000
	 * → 204 No Content
	 * </pre>
	 *
	 * @param eateryId
	 *            the eatery UUID
	 * @param flaggerId
	 *            the reporting user's UUID (from X-User-Id header)
	 * @return 204 No Content on success
	 */
	@PostMapping("/{eateryId}/report-closed")
	public ResponseEntity<Void> reportEateryClosed(@Valid @PathVariable UUID eateryId,
			@RequestHeader("X-User-Id") UUID flaggerId) {
		eateryService.reportClosed(eateryId, flaggerId);
		return ResponseEntity.noContent().build();
	}
}
