package com.salob.food_service.api.food;

import com.salob.food_service.api._domain.Food;
import com.salob.food_service.api.food.dto.FoodCreationRequest;
import com.salob.food_service.api.food.dto.FoodSearchPreview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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

    /**
     * Create a new food item, or return existing if one with the same name exists.
     * Idempotent by food name.
     *
     * TODO: Future AI verification pipeline — new foods go through an AI legitimacy
     * check (e.g., "Is 'Dragon Phoenix Rice' a real dish?"). Flow:
     * 1. Check cache/DB for existing food (current behavior)
     * 2. If new, send to AI verification service
     * 3. AI returns confidence score; if below threshold, flag for human review
     * 4. Frontend receives status: "pending_verification" or "approved"
     */
    @CacheEvict(value = "food_search", allEntries = true)
    public FoodSearchPreview createFood(FoodCreationRequest req) {
        String trimmedName = req.foodName().trim();

        // Check if food already exists (idempotent)
        List<Food> existing = foodRepo.findByLabelContainingIgnoreCase(trimmedName);
        for (Food f : existing) {
            if (f.getLabel().equalsIgnoreCase(trimmedName)) {
                log.info("Food '{}' already exists, returning existing record", trimmedName);
                return FoodSearchPreview.builder()
                        .foodId(f.getId())
                        .foodName(f.getLabel())
                        .photoUrl("")
                        .build();
            }
        }

        // Create new food
        Food newFood = Food.builder()
                .label(trimmedName)
                .photoObjKey("")
                .build();
        Food saved = foodRepo.save(newFood);
        log.info("Created new food: {} (id={})", trimmedName, saved.getId());

        return FoodSearchPreview.builder()
                .foodId(saved.getId())
                .foodName(saved.getLabel())
                .photoUrl("")
                .build();
    }
}
