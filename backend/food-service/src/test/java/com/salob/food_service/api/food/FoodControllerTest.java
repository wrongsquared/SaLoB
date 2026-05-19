package com.salob.food_service.api.food;

import com.salob.food_service.api.food.dto.FoodSearchPreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FoodControllerTest {

    @Mock
    private FoodService foodService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FoodController controller = new FoodController(foodService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchForFood_returnsOk() throws Exception {
        FoodSearchPreview dto = FoodSearchPreview.builder()
                .foodId(UUID.randomUUID())
                .foodName("Chicken Rice")
                .photoUrl("")
                .build();

        when(foodService.searchForFood("chicken")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/foods/search")
                        .param("search", "chicken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foodName").value("Chicken Rice"));
    }

    @Test
    void searchForFood_whenNoResults_returnsOkEmpty() throws Exception {
        when(foodService.searchForFood("nothing")).thenReturn(List.of());

        mockMvc.perform(get("/api/foods/search")
                        .param("search", "nothing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
