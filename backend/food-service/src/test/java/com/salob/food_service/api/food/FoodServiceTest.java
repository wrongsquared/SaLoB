package com.salob.food_service.api.food;

import com.salob.food_service.api._domain.Food;
import com.salob.food_service.api.food.dto.FoodSearchPreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodRepository foodRepo;

    private FoodService foodService;

    @BeforeEach
    void setUp() {
        foodService = new FoodService(foodRepo);
    }

    @Test
    void searchForFood_returnsMatchingResults() {
        UUID foodId = UUID.randomUUID();
        Food food = Food.builder().label("Chicken Rice").build();
        food.setId(foodId);

        when(foodRepo.findByLabelContainingIgnoreCase("chicken")).thenReturn(List.of(food));

        List<FoodSearchPreview> results = foodService.searchForFood("chicken");

        assertEquals(1, results.size());
        assertEquals(foodId, results.getFirst().getFoodId());
        assertEquals("Chicken Rice", results.getFirst().getFoodName());
        verify(foodRepo).findByLabelContainingIgnoreCase("chicken");
    }

    @Test
    void searchForFood_whenNoMatch_returnsEmptyList() {
        when(foodRepo.findByLabelContainingIgnoreCase("nonexistent")).thenReturn(List.of());

        List<FoodSearchPreview> results = foodService.searchForFood("nonexistent");

        assertTrue(results.isEmpty());
        verify(foodRepo).findByLabelContainingIgnoreCase("nonexistent");
    }
}
