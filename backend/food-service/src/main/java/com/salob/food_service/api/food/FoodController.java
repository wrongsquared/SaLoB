package com.salob.food_service.api.food;

import com.salob.food_service.api.food.dto.FoodSearchPreview;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {
    private final FoodService foodService;

    @RequestMapping("/search")
    public ResponseEntity<List<FoodSearchPreview>> searchForFood(@RequestParam String search) {
        return ResponseEntity.ok(foodService.searchForFood(search));
    }
}
