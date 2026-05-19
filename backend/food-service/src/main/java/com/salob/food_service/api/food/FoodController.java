package com.salob.food_service.api.food;

import com.salob.food_service.api.food.dto.FoodCreationRequest;
import com.salob.food_service.api.food.dto.FoodSearchPreview;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {
    private final FoodService foodService;

    @RequestMapping("/search")
    public ResponseEntity<List<FoodSearchPreview>> searchForFood(@Valid @RequestParam String search) {
        return ResponseEntity.ok(foodService.searchForFood(search));
    }

    /**
     * Create a new food item. Idempotent — returns existing food if name matches.
     * Example: POST /api/foods with body {"foodName": "Chicken Rice"}
     */
    @PostMapping
    public ResponseEntity<FoodSearchPreview> createFood(@Valid @RequestBody FoodCreationRequest req) {
        return ResponseEntity.ok(foodService.createFood(req));
    }
}
