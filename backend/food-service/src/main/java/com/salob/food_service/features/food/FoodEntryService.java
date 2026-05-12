package com.salob.food_service.features.food;

import com.salob.food_service.features.eatery.dto.FoodDetailedDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodEntryService {
    private final FoodEntryRepository foodEntryRepository;

    public FoodDetailedDTO getFoodEntryDetailed(UUID foodEntryId, Instant startDate) {
        return FoodDetailedDTO.builder().build();
    }
}
