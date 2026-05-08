package com.salob.food_service.seeding;

import com.salob.food_service.features.eatery.EateryRepository;
import com.salob.food_service.features.eatery.EateryTypeRepository;
import com.salob.food_service.features.food.FoodEntryRepository;
import com.salob.food_service.features.food.FoodEntryVoteRepository;
import com.salob.food_service.features.food.FoodRepository;
import com.salob.food_service.features.eatery.domain.Eatery;
import com.salob.food_service.features.eatery.domain.EateryType;
import com.salob.food_service.features.food.domain.Food;
import com.salob.food_service.features.food.domain.FoodEntry;
import com.salob.food_service.seeding.seeders.EaterySeeder;
import com.salob.food_service.seeding.seeders.EateryTypeSeeder;
import com.salob.food_service.seeding.seeders.FoodEntrySeeder;
import com.salob.food_service.seeding.seeders.FoodEntryVoteSeeder;
import com.salob.food_service.seeding.seeders.FoodSeeder;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Orchestrates all seeders in explicit order.
 *
 * Seeding order matters:
 * 1. EateryTypes (no dependencies)
 * 2. Eateries (depends on types)
 * 3. Foods (no dependencies)
 * 4. FoodEntries (depends on foods + eateries)
 * 5. FoodEntryVotes (depends on food entries + user IDs)
 *
 * Reset behavior:
 * - If app.seed.reset=true, deletes all data before reseeding (fast for stress tests)
 * - Otherwise, idempotent seeding (safe to run multiple times)
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {
    private final EateryTypeSeeder eateryTypeSeeder;
    private final EaterySeeder eaterySeeder;
    private final FoodSeeder foodSeeder;
    private final FoodEntrySeeder foodEntrySeeder;
    private final FoodEntryVoteSeeder foodEntryVoteSeeder;
    private final FoodEntryRepository foodEntryRepository;
    private final EateryRepository eateryRepository;
    private final FoodRepository foodRepository;
    private final EateryTypeRepository eateryTypeRepository;
    private final FoodEntryVoteRepository foodEntryVoteRepository;

    @Override
    public void run(String... args) {
        resetDatabase();
        log.info("Starting seeding process...");

        List<UUID> userIDs = generateDemoUserIds(80);
        List<EateryType> eateryTypes = eateryTypeSeeder.seed();
        List<Eatery> eateries = eaterySeeder.seed(eateryTypes);
        List<Food> foods = foodSeeder.seed();
        List<FoodEntry> foodEntries = foodEntrySeeder.seed(userIDs, foods, eateries);
        foodEntryVoteSeeder.seed(userIDs, foodEntries);

        log.info("Seeding complete");
    }

    private void resetDatabase() {
        try {
            foodEntryVoteRepository.deleteAllInBatch();
            log.debug("Deleted food entry votes");
        } catch (Exception e) {
            log.warn("Failed to delete food entry votes: {}", e.getMessage());
        }

        try {
            foodEntryRepository.deleteAllInBatch();
            log.debug("Deleted food entries");
        } catch (Exception e) {
            log.warn("Failed to delete food entries: {}", e.getMessage());
        }

        try {
            eateryRepository.deleteAllInBatch();
            log.debug("Deleted eateries");
        } catch (Exception e) {
            log.warn("Failed to delete eateries: {}", e.getMessage());
        }

        try {
            foodRepository.deleteAllInBatch();
            log.debug("Deleted foods");
        } catch (Exception e) {
            log.warn("Failed to delete foods: {}", e.getMessage());
        }

        try {
            eateryTypeRepository.deleteAllInBatch();
            log.debug("Deleted eatery types");
        } catch (Exception e) {
            log.warn("Failed to delete eatery types: {}", e.getMessage());
        }
    }

    /**
     * Generate synthetic user IDs for demo voting.
     *
     * In production, these would come from an actual User entity.
     * For testing, we generate random UUIDs.
     */
    private List<UUID> generateDemoUserIds(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> UUID.randomUUID())
            .toList();
    }
}
