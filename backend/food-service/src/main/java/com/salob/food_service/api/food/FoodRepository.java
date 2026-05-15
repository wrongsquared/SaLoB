package com.salob.food_service.api.food;

import com.salob.food_service.api._domain.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FoodRepository extends JpaRepository<Food, UUID> {
    boolean existsByLabel(String label);
    List<Food> findByLabelContainingIgnoreCase(String label); // iLike
}
