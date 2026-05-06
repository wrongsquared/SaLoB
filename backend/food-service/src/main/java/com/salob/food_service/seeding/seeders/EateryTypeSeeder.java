package com.salob.food_service.seeding.seeders;

import com.salob.food_service.api.eatery.EateryTypeRepository;
import com.salob.food_service.domain.EateryType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EateryTypeSeeder {

    private final EateryTypeRepository eateryTypeRepository;

    @Transactional
    public void seed() {
        List.of(
            "Hawker Stall",
            "Cafe",
            "Restaurant",
            "Food Court",
            "Bakery",
            "Bistro",
            "Kopitiam",
            "Bubble Tea Shop",
            "Dessert Shop",
            "Fast Food"
        ).forEach(this::seedTypeIfMissing);
    }

    private void seedTypeIfMissing(String label) {
        if (eateryTypeRepository.existsByLabel(label)) {
            return;
        }

        eateryTypeRepository.save(EateryType.builder().label(label).build());
    }
}
