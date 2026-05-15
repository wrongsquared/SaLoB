package com.salob.food_service.api.food;

import com.salob.food_service.api.food.dto.FoodSearchPreview;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {
    private final FoodRepository foodRepo;

    @Cacheable(value = "food_search", key = "#search")
    public List<FoodSearchPreview> searchForFood(String search) {
        return foodRepo.findByLabelContainingIgnoreCase(search)
                .stream()
                .map(f -> FoodSearchPreview.builder()
                        .foodId(f.getId())
                        .foodName(f.getLabel())
                        .photoUrl("")
                        .build())
                .toList();
    }
}
