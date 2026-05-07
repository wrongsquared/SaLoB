package com.salob.food_service.api.food;

import com.salob.food_service.domain.FoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FoodEntryRepository extends JpaRepository<FoodEntry, UUID> { }
