package com.salob.food_service.seeding.seeders;

import com.salob.food_service.api.food.FoodEntryRepository;
import com.salob.food_service.domain.Eatery;
import com.salob.food_service.domain.Food;
import com.salob.food_service.domain.FoodEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class FoodEntrySeeder {
    private static final int MIN_ENTRIES_PER_EATERY = 3;
    private static final int MAX_ENTRIES_PER_EATERY = 30;

    private static final int MIN_PRICE_CENTS = 300;
    private static final int MAX_PRICE_CENTS = 10000;

    private final FoodEntryRepository foodEntryRepository;

    @Transactional
    public List<FoodEntry> seed(List<UUID> userIDs, List<Food> foods, List<Eatery> eateries) {
        if (foods.isEmpty() || eateries.isEmpty()) {
            log.warn("Skipping food entry seeding because foods or eateries are missing");
            return new ArrayList<>();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<FoodEntry> entries = new ArrayList<>(eateries.size() * 3); // Random number I chose

        int minEntriesPerEatery = Math.min(foods.size(), MIN_ENTRIES_PER_EATERY);
        int maxEntriesPerEatery = Math.min(foods.size(), MAX_ENTRIES_PER_EATERY);
        for (Eatery eatery : eateries) {
            int numEntriesForEatery = random.nextInt(minEntriesPerEatery, maxEntriesPerEatery);

            for (int i = 0; i < numEntriesForEatery; i++) {
                Food food = foods.get(random.nextInt(foods.size()));
                int priceCents = random.nextInt(MIN_PRICE_CENTS, MAX_PRICE_CENTS + 1);

                entries.add(
                    FoodEntry.builder()
                        .eatery(eatery)
                        .food(food)
                        .sgCents(priceCents)
                        .submitterId(userIDs.get(random.nextInt(userIDs.size())))
                        .build()
                );
            }
        }
        return foodEntryRepository.saveAll(entries);
    }
}
