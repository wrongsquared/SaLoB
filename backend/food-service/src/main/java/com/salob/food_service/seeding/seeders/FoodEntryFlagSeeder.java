package com.salob.food_service.seeding.seeders;

import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api._domain.FoodEntryFlag;
import com.salob.food_service.api.food_entry_flag.FoodEntryFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FoodEntryFlagSeeder {
    private final FoodEntryFlagRepository foodEntryFlagRepo;

    @Transactional
    public List<FoodEntryFlag> seed(List<UUID> userIDs, List<FoodEntry> foodEntries) {
        List<FoodEntryFlag> foodEntryFlags = new ArrayList<>();

        var random = new Random();
        for (FoodEntry foodEntry : foodEntries) {
            int numFlags = random.nextInt(20);
            for (int i = 0; i < numFlags; i++) {
                UUID flaggerId = userIDs.get(random.nextInt(userIDs.size()));

                FoodEntryFlag flag = FoodEntryFlag.builder()
                    .foodEntry(foodEntry)
                    .flaggerId(flaggerId)
                    .reason("Inappropriate content " + (i + 1))
                    .build();
                foodEntryFlags.add(flag);
            }
        }

        return foodEntryFlagRepo.saveAll(foodEntryFlags);
    }
}
