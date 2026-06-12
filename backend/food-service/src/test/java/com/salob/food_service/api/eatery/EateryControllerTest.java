package com.salob.food_service.api.eatery;

import com.salob.food_service.api.eatery.dto.EateryMapDTO;
import com.salob.food_service.api.eatery.dto.EateryPreviewDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * =============================================================================
 * WHAT THIS TEST TEACHES
 * =============================================================================
 *
 * STANDALONE MockMvc SETUP
 *
 * In Spring Boot 4.x, @WebMvcTest and @MockitoBean were REMOVED. The new
 * approach is "standalone MockMvc setup" — we create the controller manually
 * (via new + constructor) and configure MockMvc to use it directly.
 *
 * This is actually BETTER than @WebMvcTest because:
 *   1. NO Spring context at all — zero startup overhead (~0.01s)
 *   2. Full control over what's wired
 *   3. No hidden beans, no surprises
 *   4. Same plain-new-instantiation pattern as service tests
 *
 * The trade-off: we don't get Spring's @Valid on request params and
 * @RestControllerAdvice exception handlers automatically. We can add
 * them if needed by calling .setValidator() and .setControllerAdvice().
 *
 * Note: Rate limiting is now handled at the API gateway layer, so these
 * controller tests no longer mock or test it.
 * =============================================================================
 */

@ExtendWith(MockitoExtension.class)
class EateryControllerTest {

	@Mock
	private EateryService eateryService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		EateryController controller = new EateryController(eateryService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	// =========================================================================
	// SECTION 1: GET /api/eateries/within-bounds
	// =========================================================================

	@Test
	void getEateriesWithinBounds_returnsOk_withEateryList() throws Exception {
		UUID id = UUID.randomUUID();
		EateryMapDTO dto = new EateryMapDTO(id, "Test Hawker", 1.3, 103.8, "Hawker Stall");

		when(eateryService.findEateriesWithinBounds(1.27, 1.32, 103.8, 103.86)).thenReturn(List.of(dto));

		mockMvc.perform(get("/api/eateries/within-bounds").param("minLat", "1.27").param("maxLat", "1.32")
				.param("minLon", "103.8").param("maxLon", "103.86").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Test Hawker"))
				.andExpect(jsonPath("$[0].latitude").value(1.3)).andExpect(jsonPath("$[0].longitude").value(103.8));
	}

	@Test
	void getEateriesWithinBounds_whenInvalidBounds_returns400() throws Exception {
		mockMvc.perform(get("/api/eateries/within-bounds").param("minLat", "1.32").param("maxLat", "1.32")
				.param("minLon", "103.8").param("maxLon", "103.86")).andExpect(status().isBadRequest());
	}

	// =========================================================================
	// SECTION 2: GET /api/eateries/{eateryId}
	// =========================================================================

	@Test
	void getEateryDetailed_returnsOk() throws Exception {
		UUID id = UUID.randomUUID();
		var dto = new com.salob.food_service.api.eatery.dto.EateryDetailedDTO(id, "Test Hawker", "1 Test Street",
				"Hawker Stall", "https://photo.url", List.of(), null);

		when(eateryService.getEateryDetailed(id, null)).thenReturn(dto);

		mockMvc.perform(get("/api/eateries/{eateryId}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Test Hawker"))
				.andExpect(jsonPath("$.typeLabel").value("Hawker Stall"));
	}

	// =========================================================================
	// SECTION 3: GET /api/eateries/search
	// =========================================================================

	@Test
	void searchForEateries_returnsOk() throws Exception {
		var dto = new EateryPreviewDTO(UUID.randomUUID(), "Test Hawker", "1 Test Street");

		when(eateryService.searchForEateries("test")).thenReturn(List.of(dto));

		mockMvc.perform(get("/api/eateries/search").param("search", "test").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Test Hawker"))
				.andExpect(jsonPath("$[0].address").value("1 Test Street"));
	}

	@Test
	void searchForEateries_whenNoResults_returns204() throws Exception {
		when(eateryService.searchForEateries("nothing")).thenReturn(List.of());

		mockMvc.perform(get("/api/eateries/search").param("search", "nothing")).andExpect(status().isNoContent());
	}
}
