package com.salob.food_service.api.eatery;

import com.salob.food_service.api._helpers.RateLimiter;
import com.salob.food_service.api.eatery.dto.EateryMapDTO;
import com.salob.food_service.api.eatery.dto.EateryPreviewDTO;
import com.salob.food_service.common.Utils;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
 * =============================================================================
 */

@ExtendWith(MockitoExtension.class)
class EateryControllerTest {

    @Mock
    private EateryService eateryService;

    @Mock
    private RateLimiter rateLimiter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        /*
         * Manual controller instantiation — same pattern as service tests.
         * Constructor injection via @RequiredArgsConstructor.
         */
        EateryController controller = new EateryController(eateryService, rateLimiter);

        /*
         * standaloneSetup creates MockMvc with JUST this controller.
         * No Spring context, no filters, no interceptors (unless we add them).
         * This is the most lightweight test setup possible.
         */
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // =========================================================================
    // SECTION 1: GET /api/eateries/within-bounds
    // =========================================================================

    @Test
    void getEateriesWithinBounds_returnsOk_withEateryList() throws Exception {
        UUID id = UUID.randomUUID();
        /*
         * EateryMapDTO is a class with @Getter (Lombok).
         * Its JSON property names come from getter names:
         *   getEateryId() → "eateryId"
         *   getName() → "name"
         *   getLatitude() → "latitude"
         */
        EateryMapDTO dto = new EateryMapDTO(id, "Test Hawker", 1.3, 103.8, "Hawker Stall");

        when(rateLimiter.isRequestAllowed(anyString())).thenReturn(true);
        when(eateryService.findEateriesWithinBounds(1.27, 1.32, 103.8, 103.86))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/eateries/within-bounds")
                        .param("minLat", "1.27")
                        .param("maxLat", "1.32")
                        .param("minLon", "103.8")
                        .param("maxLon", "103.86")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Hawker"))
                .andExpect(jsonPath("$[0].latitude").value(1.3))
                .andExpect(jsonPath("$[0].longitude").value(103.8));
    }

    @Test
    void getEateriesWithinBounds_whenRateLimited_returns429() throws Exception {
        when(rateLimiter.isRequestAllowed(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/eateries/within-bounds")
                        .param("minLat", "1.27")
                        .param("maxLat", "1.32")
                        .param("minLon", "103.8")
                        .param("maxLon", "103.86"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void getEateriesWithinBounds_whenInvalidBounds_returns400() throws Exception {
        /*
         * Rate limiter check happens BEFORE bounds validation in the controller.
         * Without this stub, the unstubbed mock returns false → 429.
         */
        when(rateLimiter.isRequestAllowed(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/eateries/within-bounds")
                        .param("minLat", "1.32")
                        .param("maxLat", "1.32")
                        .param("minLon", "103.8")
                        .param("maxLon", "103.86"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // SECTION 2: GET /api/eateries/{eateryId}
    // =========================================================================

    @Test
    void getEateryDetailed_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        /*
         * EateryDetailedDTO is a record with @Builder (Lombok).
         * Records have accessor methods (not getters), but Jackson
         * recognizes record components as properties automatically.
         * JSON: "name" (not "getName"), "typeLabel" (not "getTypeLabel").
         */
        var dto = new com.salob.food_service.api.eatery.dto.EateryDetailedDTO(
                id, "Test Hawker", "1 Test Street", "Hawker Stall",
                "https://photo.url", List.of());

        when(rateLimiter.isRequestAllowed(anyString())).thenReturn(true);
        when(eateryService.getEateryDetailed(id)).thenReturn(dto);

        mockMvc.perform(get("/api/eateries/{eateryId}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Hawker"))
                .andExpect(jsonPath("$.typeLabel").value("Hawker Stall"));
    }

    // =========================================================================
    // SECTION 3: GET /api/eateries/search
    // =========================================================================

    @Test
    void searchForEateries_returnsOk() throws Exception {
        /*
         * EateryPreviewDTO is a record: public record(UUID eateryId, String name, String address).
         * Jackson serializes record components directly.
         */
        var dto = new EateryPreviewDTO(UUID.randomUUID(), "Test Hawker", "1 Test Street");

        when(eateryService.searchForEateries("test")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/eateries/search")
                        .param("search", "test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Hawker"))
                .andExpect(jsonPath("$[0].address").value("1 Test Street"));
    }

    @Test
    void searchForEateries_whenNoResults_returns204() throws Exception {
        when(eateryService.searchForEateries("nothing")).thenReturn(List.of());

        mockMvc.perform(get("/api/eateries/search")
                        .param("search", "nothing"))
                .andExpect(status().isNoContent());
    }
}
