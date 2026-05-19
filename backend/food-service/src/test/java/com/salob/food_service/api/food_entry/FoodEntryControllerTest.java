package com.salob.food_service.api.food_entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salob.food_service.api._helpers.RateLimiter;
import com.salob.food_service.api.food_entry.dto.FoodEntryDetailedDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryHistoricalDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntrySubmissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FoodEntryControllerTest {

    @Mock
    private FoodEntryService foodEntryService;

    @Mock
    private RateLimiter rateLimiter;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        FoodEntryController controller = new FoodEntryController(foodEntryService, rateLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getHistoricalData_returnsOk() throws Exception {
        UUID entryId = UUID.randomUUID();
        when(rateLimiter.isRequestAllowed(anyString())).thenReturn(true);

        FoodEntryHistoricalDTO dto = FoodEntryHistoricalDTO.builder()
                .foodName("Chicken Rice")
                .sgCentsConsensusPrice(400)
                .eateryId(UUID.randomUUID())
                .eateryAddress("1 Test Street")
                .availableDates(List.of())
                .benchmarkDateEntries(List.of())
                .consensusEntry(null)
                .build();
        when(foodEntryService.getFoodEntryHistoricalData(eq(entryId), any(Instant.class)))
                .thenReturn(dto);

        mockMvc.perform(get("/api/food-entries/historical-data/{foodEntryId}", entryId)
                        .param("startDate", "2025-01-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foodName").value("Chicken Rice"))
                .andExpect(jsonPath("$.sgCentsConsensusPrice").value(400));
    }

    @Test
    void getHistoricalData_whenStartDateInFuture_returns400() throws Exception {
        when(rateLimiter.isRequestAllowed(anyString())).thenReturn(true);

        // year 3000 — definitely in the future
        mockMvc.perform(get("/api/food-entries/historical-data/{foodEntryId}", UUID.randomUUID())
                        .param("startDate", "3000-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(foodEntryService);
    }

    @Test
    void getFoodEntryDetails_returnsOk() throws Exception {
        UUID entryId = UUID.randomUUID();
        when(rateLimiter.isRequestAllowed(anyString())).thenReturn(true);

        FoodEntryDetailedDTO dto = FoodEntryDetailedDTO.builder()
                .foodEntryId(entryId)
                .submitterId(UUID.randomUUID())
                .submitterUsername("testuser")
                .build();
        when(foodEntryService.getFoodEntryDetailed(entryId)).thenReturn(dto);

        mockMvc.perform(get("/api/food-entries/{foodEntryId}/details", entryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foodEntryId").isString())
                .andExpect(jsonPath("$.submitterUsername").value("testuser"));
    }

    @Test
    void submitFoodEntry_returnsOk() throws Exception {
        UUID submitterId = UUID.randomUUID();
        FoodEntrySubmissionRequest req = new FoodEntrySubmissionRequest(
                UUID.randomUUID(), UUID.randomUUID(), 500);

        // POST /submit reads X-User-Id from header
        mockMvc.perform(post("/api/food-entries/submit")
                        .header("X-User-Id", submitterId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(foodEntryService).submitFoodEntry(eq(submitterId), any(FoodEntrySubmissionRequest.class));
    }
}
