package com.salob.food_service.seeding.seeders;

import com.salob.food_service.api._domain.Eatery;
import com.salob.food_service.api._domain.Food;
import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api.food_entry.FoodEntryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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

	/*
	 * Seeder strategy — each eatery gets multiple entries for a subset of foods so
	 * the historical price chart (datePrices) has more than one data point per
	 * (food, eatery) pair. Dates are uniform-random across the past year.
	 */
	private static final int MIN_FOODS_PER_EATERY = 2;
	private static final int MAX_FOODS_PER_EATERY = 5;
	private static final int MIN_ENTRIES_PER_FOOD = 3;
	private static final int MAX_ENTRIES_PER_FOOD = 6;

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
		List<FoodEntry> entries = new ArrayList<>(eateries.size() * 3);
		Instant now = Instant.now();

		List<Food> shuffledFoods = new ArrayList<>(foods);
		Collections.shuffle(shuffledFoods, new Random(random.nextLong()));

		int maxFoods = Math.min(MAX_FOODS_PER_EATERY, shuffledFoods.size());
		int minFoods = Math.min(MIN_FOODS_PER_EATERY, maxFoods);

		for (Eatery eatery : eateries) {
			int numFoods = random.nextInt(minFoods, maxFoods + 1);

			for (int fi = 0; fi < numFoods; fi++) {
				Food food = shuffledFoods.get(fi);

				int entriesPerFood = random.nextInt(MIN_ENTRIES_PER_FOOD, MAX_ENTRIES_PER_FOOD + 1);

				for (int i = 0; i < entriesPerFood; i++) {
					int priceCents = random.nextInt(MIN_PRICE_CENTS, MAX_PRICE_CENTS + 1);

					long offset = (long) (random.nextDouble() * 365L * 86400);
					Instant createdAt = now.minusSeconds(offset);

					FoodEntry entry = FoodEntry.builder().eatery(eatery).food(food).sgCents(priceCents)
							.submitterId(userIDs.get(random.nextInt(userIDs.size()))).build();
					entry.setCreatedAt(createdAt);
					entries.add(entry);
				}
			}
		}

		return foodEntryRepository.saveAll(entries);
	}
}
