package com.salob.food_service.seeding;

import com.salob.food_service.api.eatery.EateryRepository;
import com.salob.food_service.api.eatery.EateryTypeRepository;
import com.salob.food_service.api.food.FoodEntryRepository;
import com.salob.food_service.api.food.FoodRepository;
import com.salob.food_service.domain.Eatery;
import com.salob.food_service.domain.EateryType;
import com.salob.food_service.domain.Food;
import com.salob.food_service.seeding.seeders.EaterySeeder;
import com.salob.food_service.seeding.seeders.EateryTypeSeeder;
import com.salob.food_service.seeding.seeders.FoodEntrySeeder;
import com.salob.food_service.seeding.seeders.FoodSeeder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
    //---------------------------------------------------
    private final FoodEntryRepository foodEntryRepository;
    private final EateryRepository eateryRepository;
    private final FoodRepository foodRepository;
    private final EateryTypeRepository eateryTypeRepository;

    @Value("${app.seed.reset:false}")
    private boolean resetData;

    @Override
    public void run(String... args) {
        log.info("Running seeders in explicit order");

        if (resetData) {
            resetDatabase();
        }

        List<EateryType> eateryTypes = eateryTypeSeeder.seed();
        List<Eatery> eateries = eaterySeeder.seed(eateryTypes);
        List<Food> foods = foodSeeder.seed();
        foodEntrySeeder.seed(foods, eateries);

        log.info("Seeding complete");
    }

    private void resetDatabase() {
        log.warn("Reset flag enabled - deleting existing seed data");
        // Delete children first; add additional deletes here if new dependent tables are introduced.
        foodEntryRepository.deleteAllInBatch();
        eateryRepository.deleteAllInBatch();
        foodRepository.deleteAllInBatch();
        eateryTypeRepository.deleteAllInBatch();
    }
}
