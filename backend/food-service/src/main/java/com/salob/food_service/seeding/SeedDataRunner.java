package com.salob.food_service.seeding;

import com.salob.food_service.seeding.seeders.EaterySeeder;
import com.salob.food_service.seeding.seeders.EateryTypeSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void run(String... args) {
        log.info("Running seeders in explicit order");
        eateryTypeSeeder.seed();
        eaterySeeder.seed();
        log.info("Seeding complete");
    }
}
