package com.salob.food_service.api.food;

import com.salob.food_service.api.food.dto.FoodSearchPreview;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
