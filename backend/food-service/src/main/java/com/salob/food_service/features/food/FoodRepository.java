package com.salob.food_service.features.food;

import com.salob.food_service.features.food.domain.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FoodRepository extends JpaRepository<Food, UUID> {
    boolean existsByLabel(String label);
}
